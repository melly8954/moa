package com.moa.api.service.party;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.moa.api.dto.party.BanReason;
import com.moa.api.dto.party.JoinPolicy;
import com.moa.api.dto.party.JoinRequestApplied;
import com.moa.api.dto.party.JoinRequestDecided;
import com.moa.api.dto.party.PartyJoinRequestDto;
import com.moa.api.dto.party.PendingRequestCount;
import com.moa.api.entity.party.Party;
import com.moa.api.entity.party.PartyBan;
import com.moa.api.entity.party.PartyJoinRequest;
import com.moa.api.entity.party.PartyMember;
import com.moa.api.repository.reader.party.PartyReader;
import com.moa.api.repository.writer.party.PartyWriter;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;

import lombok.RequiredArgsConstructor;

/**
 * 승인 파티의 참가 신청 (specs/008-party US5). 신청 확인은 파티 락 안에서 한다 (research.md R6).
 * 승인해서 멤버가 되는 일은 PartyMemberService.admit이 하고, Application이 둘을 묶는다.
 */
@Service
@RequiredArgsConstructor
public class PartyJoinRequestService {

	private final PartyReader partyReader;
	private final PartyWriter partyWriter;

	/**
	 * 신청을 받는다 (US5-AC1). 정원을 마감보다 먼저 본다 (contracts/party-api.md "멤버가 되는 모든 길").
	 *
	 * @throws ServiceException 없는 파티(멤버가 아닌 회원의 비공개 파티 포함) R001, 승인 방식이 아님 B112, 참가 제한 B104,
	 *     이미 속함 B103, 대기 중인 신청이 있음 B108, 정원 B101, 마감 B102, 포지션 B105·B106
	 */
	public JoinRequestApplied apply(Long partyId, Long userId, String positionCode, String tierCode) {
		Party party = partyReader.findForUpdate(partyId).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
		if (party.getJoinPolicy() != JoinPolicy.APPROVAL) {
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
		if (partyReader.findPendingRequest(partyId, userId).isPresent()) {
			throw new ServiceException(ErrorCode.PARTY_JOIN_REQUEST_PENDING);
		}
		List<PartyMember> members = partyReader.findMembers(partyId);
		if (members.size() >= party.getCapacity()) {
			throw new ServiceException(ErrorCode.PARTY_FULL);
		}
		if (!party.isRecruiting()) {
			throw new ServiceException(ErrorCode.PARTY_CLOSED);
		}
		String position = party.isPositionEnabled()
			? PartyPositions.requireOpen(partyReader.findPositions(partyId), members, positionCode)
			: null;
		PartyJoinRequest request;
		try {
			request = partyWriter.saveRequest(new PartyJoinRequest(partyId, userId, position, tierCode));
		} catch (DataIntegrityViolationException e) {
			// 같은 회원이 동시에 두 번 신청함 (uk_party_join_request_party_id_user_id_pending_key, US5-AC7)
			throw new ServiceException(ErrorCode.PARTY_JOIN_REQUEST_PENDING);
		}
		return new JoinRequestApplied(request.getId(), partyId, party.getHostUserId(), party.getTitle());
	}

	/**
	 * 방장의 신청 목록. 대기 중인 것만 신청순이다 (US5-AC2).
	 *
	 * @throws ServiceException 없는 파티 R001, 방장이 아니면 A101
	 */
	public List<PartyJoinRequestDto> getPending(Long partyId, Long hostUserId) {
		Party party = partyReader.find(partyId).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
		requireHost(party, hostUserId);
		return partyReader.findPendingRequests(partyId).stream().map(PartyJoinRequestDto::from).toList();
	}

	/**
	 * 신청을 승인으로 바꾼다 (US5-AC3). 멤버로 들이는 일은 이어서 Application이 admit(via=APPROVAL)으로 한다.
	 * 승인을 먼저 적어, admit이 그 회원의 나머지 대기 신청만 취소하게 한다.
	 *
	 * @throws ServiceException 없는 파티 R001, 방장이 아니면 A101, 이 파티의 대기 중인 신청이 아니면 R001
	 */
	public JoinRequestDecided approve(Long partyId, Long hostUserId, Long joinRequestId) {
		Party party = requireHostLocked(partyId, hostUserId);
		PartyJoinRequest request = requirePending(partyId, joinRequestId);
		request.approve(LocalDateTime.now());
		return new JoinRequestDecided(PartyJoinRequestDto.from(request), party.getTitle());
	}

	/**
	 * 신청을 거절한다 (US5-AC4). 그 회원은 이 파티에 다시 들어오지 못한다 (FR-013).
	 *
	 * @throws ServiceException 없는 파티 R001, 방장이 아니면 A101, 이 파티의 대기 중인 신청이 아니면 R001
	 */
	public JoinRequestDecided reject(Long partyId, Long hostUserId, Long joinRequestId) {
		Party party = requireHostLocked(partyId, hostUserId);
		PartyJoinRequest request = requirePending(partyId, joinRequestId);
		request.reject(LocalDateTime.now());
		if (!partyReader.isBanned(partyId, request.getUserId())) {
			partyWriter.saveBan(new PartyBan(partyId, request.getUserId(), BanReason.REJECTED));
		}
		return new JoinRequestDecided(PartyJoinRequestDto.from(request), party.getTitle());
	}

	/**
	 * 신청한 회원이 자기 대기 신청을 취소한다 (US5-AC6).
	 *
	 * @return 신청 목록이 바뀐 파티
	 * @throws ServiceException 이 파티에 대기 중인 내 신청이 없으면(해산 포함) R001
	 */
	public Long cancelMine(Long partyId, Long userId) {
		partyReader.find(partyId).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
		PartyJoinRequest request = partyReader.findPendingRequest(partyId, userId)
			.orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
		request.cancel(LocalDateTime.now());
		return partyId;
	}

	/**
	 * 이 회원의 대기 신청을 모두 취소한다 (회원 탈퇴, FR-031).
	 *
	 * @return 신청 목록이 바뀐 파티들
	 */
	public List<Long> cancelAllOf(Long userId) {
		LocalDateTime now = LocalDateTime.now();
		List<Long> partyIds = new ArrayList<>();
		for (PartyJoinRequest request : partyReader.findPendingRequestsOf(userId)) {
			request.cancel(now);
			partyIds.add(request.getPartyId());
		}
		return partyIds;
	}

	/** 파티마다 방장과 대기 신청 수 (JOIN_REQUESTS_CHANGED). 해산한 파티는 빠진다 */
	public List<PendingRequestCount> pendingCounts(Collection<Long> partyIds) {
		Map<Long, Party> parties = partyReader.findAll(new LinkedHashSet<>(partyIds)).stream()
			.collect(Collectors.toMap(Party::getId, Function.identity()));
		return new LinkedHashSet<>(partyIds).stream()
			.filter(parties::containsKey)
			.map(id -> new PendingRequestCount(id, parties.get(id).getHostUserId(),
				partyReader.findPendingRequests(id).size()))
			.toList();
	}

	private Party requireHostLocked(Long partyId, Long userId) {
		Party party = partyReader.findForUpdate(partyId).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
		requireHost(party, userId);
		return party;
	}

	private static void requireHost(Party party, Long userId) {
		if (!party.isHost(userId)) {
			throw new ServiceException(ErrorCode.PARTY_HOST_ONLY);
		}
	}

	private PartyJoinRequest requirePending(Long partyId, Long joinRequestId) {
		return partyReader.findRequest(joinRequestId)
			.filter(request -> request.getPartyId().equals(partyId) && request.isPending())
			.orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
	}
}
