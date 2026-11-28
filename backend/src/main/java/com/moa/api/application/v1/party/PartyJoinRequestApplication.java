package com.moa.api.application.v1.party;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.application.v1.notification.NotificationApplication;
import com.moa.api.dto.notification.NotificationType;
import com.moa.api.dto.party.JoinRequestApplied;
import com.moa.api.dto.party.JoinRequestDecided;
import com.moa.api.dto.party.MemberRef;
import com.moa.api.dto.party.PartyJoinRequestDto;
import com.moa.api.dto.party.PartyJoinRequestView;
import com.moa.api.dto.party.PartyView;
import com.moa.api.service.party.PartyJoinRequestService;
import com.moa.api.service.party.PartyMemberService;
import com.moa.api.service.user.UserService;

import lombok.RequiredArgsConstructor;

/**
 * 승인 파티의 참가 신청 (specs/008-party US5). 신청·취소·처리 뒤 방장에게 JOIN_REQUESTS_CHANGED를 보내고(US9-AC9),
 * 신청·승인·거절은 개인 알림을 같은 트랜잭션에서 남긴다 (FR-029, research.md R13). 신청 취소에는 알림이 없다.
 */
@Component
@RequiredArgsConstructor
public class PartyJoinRequestApplication {

	private final PartyJoinRequestService partyJoinRequestService;
	private final PartyApplication partyApplication;
	private final UserService userService;
	private final NotificationApplication notificationApplication;

	/**
	 * 신청한다 (US5-AC1). 방장에게 PARTY_APPLIED를 보낸다 (US5-AC9).
	 *
	 * @return 신청 ID
	 */
	@Transactional
	public Long apply(Long userId, Long partyId, String positionCode, String tierCode) {
		partyApplication.requireAllowed(userId);
		partyApplication.requireTier(partyId, tierCode);
		JoinRequestApplied applied = partyJoinRequestService.apply(partyId, userId, positionCode, tierCode);
		String name = userService.displayNames(List.of(userId)).get(userId);
		notificationApplication.notify(applied.hostUserId(), NotificationType.PARTY_APPLIED,
			name + "님이 「" + applied.title() + "」 파티에 참가를 신청했습니다", "/parties/" + partyId);
		partyApplication.publishJoinRequestsChanged(List.of(partyId));
		return applied.joinRequestId();
	}

	/** 방장의 신청 목록. 대기 중인 것만 신청순 (US5-AC2) */
	@Transactional(readOnly = true)
	public List<PartyJoinRequestView> getPending(Long hostUserId, Long partyId) {
		List<PartyJoinRequestDto> requests = partyJoinRequestService.getPending(partyId, hostUserId);
		Map<Long, String> names = userService.displayNames(requests.stream().map(PartyJoinRequestDto::userId)
			.toList());
		return requests.stream()
			.map(request -> new PartyJoinRequestView(request, new MemberRef(request.userId(),
				names.get(request.userId()))))
			.toList();
	}

	/**
	 * 승인한다 (US5-AC3). 멤버가 되는 공통 처리를 via=APPROVAL로 쓴다. 신청한 회원에게 PARTY_APPROVED를 보낸다 (US5-AC10).
	 */
	@Transactional
	public PartyView approve(Long hostUserId, Long partyId, Long joinRequestId) {
		JoinRequestDecided decided = partyJoinRequestService.approve(partyId, hostUserId, joinRequestId);
		PartyJoinRequestDto request = decided.request();
		partyApplication.requireAllowed(request.userId());
		partyApplication.admit(partyId, request.userId(), request.positionCode(), request.tierCode(),
			PartyMemberService.Via.APPROVAL);
		notificationApplication.notify(request.userId(), NotificationType.PARTY_APPROVED,
			"「" + decided.title() + "」 파티 참가가 승인되었습니다", "/parties/" + partyId);
		partyApplication.publishJoinRequestsChanged(List.of(partyId));
		return partyApplication.get(partyId, hostUserId);
	}

	/** 거절한다 (US5-AC4). 신청한 회원에게 PARTY_REJECTED를 보낸다 (US5-AC11) */
	@Transactional
	public void reject(Long hostUserId, Long partyId, Long joinRequestId) {
		JoinRequestDecided decided = partyJoinRequestService.reject(partyId, hostUserId, joinRequestId);
		notificationApplication.notify(decided.request().userId(), NotificationType.PARTY_REJECTED,
			"「" + decided.title() + "」 파티 참가 신청이 받아들여지지 않았습니다", "/recruit");
		partyApplication.publishJoinRequestsChanged(List.of(partyId));
	}

	/** 신청한 회원이 자기 대기 신청을 취소한다 (US5-AC6). 알림은 보내지 않는다 */
	@Transactional
	public void cancel(Long userId, Long partyId) {
		partyApplication.publishJoinRequestsChanged(List.of(partyJoinRequestService.cancelMine(partyId, userId)));
	}
}
