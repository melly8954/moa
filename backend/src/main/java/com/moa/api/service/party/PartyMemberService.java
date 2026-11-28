package com.moa.api.service.party;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.moa.api.dto.party.AdmitResult;
import com.moa.api.dto.party.BanReason;
import com.moa.api.dto.party.JoinPolicy;
import com.moa.api.dto.party.KickResult;
import com.moa.api.dto.party.LeaveResult;
import com.moa.api.dto.party.PartyMessageDto;
import com.moa.api.dto.party.PartyRole;
import com.moa.api.dto.party.SystemEvent;
import com.moa.api.entity.party.Party;
import com.moa.api.entity.party.PartyBan;
import com.moa.api.entity.party.PartyJoinRequest;
import com.moa.api.entity.party.PartyMember;
import com.moa.api.entity.party.PartyMessage;
import com.moa.api.repository.reader.party.PartyReader;
import com.moa.api.repository.writer.party.PartyWriter;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;

import lombok.RequiredArgsConstructor;

/**
 * 파티 멤버가 되고 빠지는 일 (specs/008-party). 모든 쓰기는 파티 락 안에서 센다 (research.md R6).
 */
@Service
@RequiredArgsConstructor
public class PartyMemberService {

	private final PartyReader partyReader;
	private final PartyWriter partyWriter;

	/** 멤버가 되는 길 (contracts/party-api.md "멤버가 되는 모든 길") */
	public enum Via {
		/** 공개 파티에 참가 */
		OPEN,
		/** 방장이 신청을 승인. 마감을 보지 않는다 (FR-008) */
		APPROVAL,
		/** 초대 코드로 입장 */
		INVITE
	}

	public boolean isMember(Long partyId, Long userId) {
		return partyReader.isMember(partyId, userId);
	}

	/** 이 회원이 지금 속한 파티 (한 번에 한 파티, FR-026) */
	public Optional<Long> findPartyIdOf(Long userId) {
		return partyReader.findMembership(userId).map(PartyMember::getPartyId);
	}

	/**
	 * 회원을 멤버로 들인다. 순서는 contracts/party-api.md "멤버가 되는 모든 길"이다. 정원을 마감보다 먼저 본다 (US3-AC3).
	 *
	 * @return 남긴 JOINED 안내와 함께 취소된 대기 신청의 파티들
	 * @throws ServiceException 해산 R001(공개 참가로 비공개 파티를 부르면 R001), 공개가 아닌 파티에 참가 B112, 참가 제한 B104,
	 *     이미 속함 B103, 정원 B101, 마감 B102, 포지션 B105·B106
	 */
	public AdmitResult admit(Long partyId, Long userId, String positionCode, String tierCode, Via via) {
		Party party = partyReader.findForUpdate(partyId).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
		if (via == Via.OPEN && party.getJoinPolicy() != JoinPolicy.OPEN) {
			// 비공개 파티는 멤버가 아니면 없는 파티로 보인다 (FR-014)
			throw new ServiceException(party.getJoinPolicy() == JoinPolicy.PRIVATE ? ErrorCode.NOT_FOUND
				: ErrorCode.PARTY_JOIN_POLICY_MISMATCH);
		}
		if (partyReader.isBanned(partyId, userId)) {
			throw new ServiceException(ErrorCode.PARTY_BANNED);
		}
		if (partyReader.findMembership(userId).isPresent()) {
			throw new ServiceException(ErrorCode.PARTY_ALREADY_JOINED);
		}
		List<PartyMember> members = partyReader.findMembers(partyId);
		if (members.size() >= party.getCapacity()) {
			throw new ServiceException(ErrorCode.PARTY_FULL);
		}
		if (via != Via.APPROVAL && !party.isRecruiting()) {
			throw new ServiceException(ErrorCode.PARTY_CLOSED);
		}
		String position = party.isPositionEnabled()
			? PartyPositions.requireOpen(partyReader.findPositions(partyId), members, positionCode)
			: null;

		LocalDateTime now = LocalDateTime.now();
		saveMember(new PartyMember(partyId, userId, PartyRole.MEMBER, position, tierCode, now));
		List<Long> cancelledPartyIds = new ArrayList<>();
		for (PartyJoinRequest request : partyReader.findPendingRequestsOf(userId)) {
			request.cancel(now);
			cancelledPartyIds.add(request.getPartyId());
		}
		if (members.size() + 1 >= party.getCapacity()) {
			party.close(false);
		}
		return new AdmitResult(saveSystem(partyId, SystemEvent.JOINED, userId), cancelledPartyIds);
	}

	/**
	 * 방장 멤버 행을 넣는다 (파티를 만들 때).
	 *
	 * @throws ServiceException 이미 다른 파티에 속하면 B103
	 */
	public void addHost(Long partyId, Long userId, String positionCode, String tierCode) {
		saveMember(new PartyMember(partyId, userId, PartyRole.HOST, positionCode, tierCode, LocalDateTime.now()));
	}

	/**
	 * 파티에 속해 있으면 막는다 (FR-026). 마지막 확인은 유니크 제약이 한다.
	 *
	 * @throws ServiceException 속해 있으면 B103
	 */
	public void requireNotInParty(Long userId) {
		if (partyReader.findMembership(userId).isPresent()) {
			throw new ServiceException(ErrorCode.PARTY_ALREADY_JOINED);
		}
	}

	/**
	 * 멤버가 나간다 (US6). 방장이면 가장 먼저 들어온 멤버가 잇고(FR-010), 마지막이면 해산한다.
	 * 자리가 나면 방장이 직접 마감한 경우가 아닐 때만 다시 모집한다 (FR-008).
	 *
	 * @throws ServiceException 해산했거나 없는 파티 R001, 멤버가 아니면 A102
	 */
	public LeaveResult leave(Long partyId, Long userId) {
		Party party = partyReader.findForUpdate(partyId).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
		PartyMember member = partyReader.findMember(partyId, userId)
			.orElseThrow(() -> new ServiceException(ErrorCode.PARTY_MEMBER_ONLY));
		partyWriter.deleteMember(member);
		List<PartyMessageDto> messages = new ArrayList<>();
		messages.add(saveSystem(partyId, SystemEvent.LEFT, userId));
		List<PartyMember> remaining = partyReader.findMembers(partyId);
		if (remaining.isEmpty()) {
			partyWriter.dissolve(party, userId);
			return new LeaveResult(partyId, party.getTitle(), messages, null, true);
		}
		Long newHostUserId = null;
		if (member.isHost()) {
			PartyMember next = remaining.getFirst();
			next.promoteToHost();
			party.changeHost(next.getUserId());
			newHostUserId = next.getUserId();
			messages.add(saveSystem(partyId, SystemEvent.HOST_CHANGED, newHostUserId));
		}
		reopenIfRoom(party, remaining.size());
		return new LeaveResult(partyId, party.getTitle(), messages, newHostUserId, false);
	}

	/**
	 * 방장이 멤버를 내보낸다 (US7-AC5). 그 회원은 이 파티에 다시 들어오지 못한다 (FR-013).
	 *
	 * @throws ServiceException 없는 파티 R001, 방장이 아니면 A101, 자기 자신이면 B113, 그 회원이 멤버가 아니면 R001
	 */
	public KickResult kick(Long partyId, Long hostUserId, Long targetUserId) {
		Party party = partyReader.findForUpdate(partyId).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
		if (!party.isHost(hostUserId)) {
			throw new ServiceException(ErrorCode.PARTY_HOST_ONLY);
		}
		if (hostUserId.equals(targetUserId)) {
			throw new ServiceException(ErrorCode.PARTY_KICK_SELF);
		}
		PartyMember target = partyReader.findMember(partyId, targetUserId)
			.orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
		partyWriter.deleteMember(target);
		partyWriter.saveBan(new PartyBan(partyId, targetUserId, BanReason.KICKED));
		PartyMessageDto message = saveSystem(partyId, SystemEvent.KICKED, targetUserId);
		reopenIfRoom(party, (int)partyReader.countMembers(partyId));
		return new KickResult(partyId, party.getTitle(), message);
	}

	/** 탈퇴했는데 멤버 행이나 대기 신청이 남은 회원 (FR-031, research R15) */
	public List<Long> findWithdrawnUserIdsWithLeftovers() {
		return partyReader.findWithdrawnUserIdsWithLeftovers();
	}

	/** 자동 마감된 파티에 자리가 나면 다시 모집한다. 방장이 직접 마감했으면 그대로다 (FR-008) */
	private static void reopenIfRoom(Party party, int memberCount) {
		if (!party.isRecruiting() && !party.isClosedByHost() && memberCount < party.getCapacity()) {
			party.reopen();
		}
	}

	private PartyMessageDto saveSystem(Long partyId, SystemEvent event, Long subjectUserId) {
		return PartyMessageDto.from(partyWriter.saveMessage(PartyMessage.system(partyId, event, subjectUserId)));
	}

	/** 한 번에 한 파티(uk_party_member_user_id)를 어기면 B103이다. 두 파티에 동시에 들어가는 경우다 (research.md R6) */
	private void saveMember(PartyMember member) {
		try {
			partyWriter.saveMember(member);
		} catch (DataIntegrityViolationException e) {
			throw new ServiceException(ErrorCode.PARTY_ALREADY_JOINED);
		}
	}
}
