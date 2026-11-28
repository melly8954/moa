package com.moa.api.service.party;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.moa.api.dto.party.PartyMessageDto;
import com.moa.api.dto.party.PartyRole;
import com.moa.api.dto.party.SystemEvent;
import com.moa.api.entity.party.Party;
import com.moa.api.entity.party.PartyMember;
import com.moa.api.entity.party.PartyMessage;
import com.moa.api.entity.party.PartyPosition;
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

	/**
	 * 회원을 멤버로 들인다. 순서는 contracts/party-api.md "멤버가 되는 모든 길"이다. 정원을 마감보다 먼저 본다 (US3-AC3).
	 *
	 * @return 남긴 JOINED 안내
	 * @throws ServiceException 해산 R001, 참가 제한 B104, 이미 속함 B103, 정원 B101, 마감 B102, 포지션 B105·B106
	 */
	public PartyMessageDto admit(Long partyId, Long userId, String positionCode, String tierCode, Via via) {
		Party party = partyReader.findForUpdate(partyId).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
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
		String position = party.isPositionEnabled() ? requireOpenPosition(partyId, positionCode, members) : null;

		LocalDateTime now = LocalDateTime.now();
		saveMember(new PartyMember(partyId, userId, PartyRole.MEMBER, position, tierCode, now));
		partyReader.findPendingRequestsOf(userId).forEach(request -> request.cancel(now));
		if (members.size() + 1 >= party.getCapacity()) {
			party.close(false);
		}
		return PartyMessageDto.from(partyWriter.saveMessage(PartyMessage.system(partyId, SystemEvent.JOINED, userId)));
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
	 * 고른 포지션에 빈자리가 있는지 본다 (FR-023).
	 *
	 * @throws ServiceException 고르지 않았으면 B106, 없는 포지션이거나 찼으면 B105(details.openPositions)
	 */
	private String requireOpenPosition(Long partyId, String positionCode, List<PartyMember> members) {
		if (positionCode == null || positionCode.isBlank()) {
			throw new ServiceException(ErrorCode.PARTY_POSITION_REQUIRED);
		}
		List<PartyPosition> positions = partyReader.findPositions(partyId);
		List<String> open = positions.stream()
			.filter(position -> filled(members, position.getPositionCode()) < position.getSlots())
			.map(PartyPosition::getPositionCode)
			.toList();
		if (!open.contains(positionCode)) {
			throw ServiceException.withDetails(ErrorCode.PARTY_POSITION_FULL, Map.of("openPositions", open));
		}
		return positionCode;
	}

	private static long filled(List<PartyMember> members, String positionCode) {
		return members.stream().filter(member -> positionCode.equals(member.getPositionCode())).count();
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
