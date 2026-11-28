package com.moa.api.application.v1.party;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.application.v1.notification.NotificationApplication;
import com.moa.api.application.v1.party.command.CreatePartyCommand;
import com.moa.api.application.v1.party.command.ModifyPartyCommand;
import com.moa.api.dto.code.CodeDto;
import com.moa.api.dto.code.CodeGroupDto;
import com.moa.api.dto.notification.NotificationType;
import com.moa.api.dto.party.AdmitResult;
import com.moa.api.dto.party.DissolveResult;
import com.moa.api.dto.party.JoinPolicy;
import com.moa.api.dto.party.KickResult;
import com.moa.api.dto.party.LeaveResult;
import com.moa.api.dto.party.MemberRef;
import com.moa.api.dto.party.MyPartySnapshot;
import com.moa.api.dto.party.MyPartyView;
import com.moa.api.dto.party.PartyCodeBook;
import com.moa.api.dto.party.PartyDto;
import com.moa.api.dto.party.PartyInviteSnapshot;
import com.moa.api.dto.party.PartyInviteView;
import com.moa.api.dto.party.PartyMemberDto;
import com.moa.api.dto.party.PartyMessageDto;
import com.moa.api.dto.party.PartySnapshot;
import com.moa.api.dto.party.PartySummarySnapshot;
import com.moa.api.dto.party.PartySummaryView;
import com.moa.api.dto.party.PartyView;
import com.moa.api.dto.party.PendingRequestCount;
import com.moa.api.service.code.CodeService;
import com.moa.api.service.party.PartyJoinRequestService;
import com.moa.api.service.party.PartyMemberService;
import com.moa.api.service.party.PartyService;
import com.moa.api.service.user.MemberActivityPolicy;
import com.moa.api.service.user.UserService;

import lombok.RequiredArgsConstructor;

/**
 * 파티 (specs/008-party). 트랜잭션, 관리자 계정 막기, 공통코드 읽기, 표시 이름 붙이기, 실시간 이벤트, 개인 알림을 맡는다.
 * 파티 규칙은 Service에 있다 (research.md R10). 신청은 PartyJoinRequestApplication, 채팅은 PartyMessageApplication이다.
 */
@Component
@RequiredArgsConstructor
public class PartyApplication {

	static final String GAME_GROUP = "GAME";
	private static final String RECRUIT_LINK = "/recruit";

	private final PartyService partyService;
	private final PartyMemberService partyMemberService;
	private final PartyJoinRequestService partyJoinRequestService;
	private final CodeService codeService;
	private final UserService userService;
	private final MemberActivityPolicy memberActivityPolicy;
	private final NotificationApplication notificationApplication;
	private final ApplicationEventPublisher eventPublisher;

	/** 구독 검사에서 쓴다 (controller/ws/party) */
	@Transactional(readOnly = true)
	public boolean isMember(Long partyId, Long userId) {
		return partyMemberService.isMember(partyId, userId);
	}

	/**
	 * 파티 화면 (US2-AC3~AC5).
	 *
	 * @param viewerUserId 로그인하지 않았으면 null
	 */
	@Transactional(readOnly = true)
	public PartyView get(Long partyId, Long viewerUserId) {
		return view(partyService.getSnapshot(partyId, viewerUserId));
	}

	/** 모집 페이지 (US2-AC1, AC2) */
	@Transactional(readOnly = true)
	public Page<PartySummaryView> getRecruitPage(String gameCode, String modeCode, Pageable pageable) {
		Page<PartySummarySnapshot> page = partyService.getRecruitPage(gameCode, modeCode, pageable);
		Map<Long, String> names = userService.displayNames(
			page.getContent().stream().map(summary -> summary.party().hostUserId()).collect(Collectors.toSet()));
		return page.map(summary -> summaryView(summary, names));
	}

	/** 파티를 만든다 (US1, US10). 만든 회원이 방장이자 첫 멤버다 */
	@Transactional
	public PartyView create(Long userId, CreatePartyCommand command) {
		requireAllowed(userId);
		PartyDto party = partyService.create(userId, command.form(), command.joinPolicy(),
			codes(command.form().gameCode()));
		partyMemberService.addHost(party.id(), userId,
			party.positionEnabled() ? command.form().hostPositionCode().trim() : null, null);
		return get(party.id(), userId);
	}

	/** 방장이 파티 정보를 고친다 (US7-AC1, US10-AC10) */
	@Transactional
	public PartyView modify(Long userId, Long partyId, ModifyPartyCommand command) {
		partyService.modify(partyId, userId, command.form(), codes(command.form().gameCode()));
		publishPartyUpdated(partyId);
		publishMembersChanged(partyId);
		return get(partyId, userId);
	}

	/** 참가 방식을 바꾼다 (US7-AC2~AC4) */
	@Transactional
	public PartyView changeJoinPolicy(Long userId, Long partyId, JoinPolicy joinPolicy) {
		if (partyService.changeJoinPolicy(partyId, userId, joinPolicy)) {
			publishJoinRequestsChanged(List.of(partyId));
		}
		publishPartyUpdated(partyId);
		return get(partyId, userId);
	}

	/** 방장이 모집을 마감한다 (US7-AC6) */
	@Transactional
	public PartyView close(Long userId, Long partyId) {
		partyService.close(partyId, userId);
		publishPartyUpdated(partyId);
		return get(partyId, userId);
	}

	/** 방장이 모집을 다시 연다 (US7-AC6) */
	@Transactional
	public PartyView reopen(Long userId, Long partyId) {
		partyService.reopen(partyId, userId);
		publishPartyUpdated(partyId);
		return get(partyId, userId);
	}

	/**
	 * 초대 코드를 새로 만든다 (US7-AC7).
	 *
	 * @return 새 코드가 든 파티
	 */
	@Transactional
	public PartyDto regenerateInviteCode(Long userId, Long partyId) {
		partyService.regenerateInviteCode(partyId, userId);
		publishPartyUpdated(partyId);
		return get(partyId, userId).party();
	}

	/** 방장이 해산한다 (US7-AC8). 방장을 뺀 멤버와 대기 신청자에게 알린다 (US7-AC11) */
	@Transactional
	public void dissolve(Long userId, Long partyId) {
		DissolveResult result = partyService.dissolve(partyId, userId);
		String title = "「" + result.title() + "」 파티가 해산되었습니다";
		result.recipients().forEach(recipient -> notificationApplication.notify(recipient,
			NotificationType.PARTY_DISSOLVED, title, RECRUIT_LINK));
		eventPublisher.publishEvent(PartyRealtimeEvent.toParty("PARTY_DISSOLVED", partyId, Map.of()));
	}

	/** 공개 파티에 참가한다 (US3) */
	@Transactional
	public PartyView join(Long userId, Long partyId, String positionCode, String tierCode) {
		requireAllowed(userId);
		requireTier(partyId, tierCode);
		admit(partyId, userId, positionCode, tierCode, PartyMemberService.Via.OPEN);
		return get(partyId, userId);
	}

	/**
	 * 초대 입장 화면 (US4).
	 *
	 * @param viewerUserId 로그인하지 않았으면 null
	 */
	@Transactional(readOnly = true)
	public PartyInviteView getInvite(String inviteCode, Long viewerUserId) {
		PartyInviteSnapshot invite = partyService.getInvite(inviteCode, viewerUserId);
		return new PartyInviteView(view(invite.snapshot()), invite.reasonCode());
	}

	/** 초대 코드로 들어온다. 참가 방식과 상관없이 바로 멤버가 된다 (US4-AC2~AC4) */
	@Transactional
	public PartyView joinByInvite(Long userId, String inviteCode, String positionCode, String tierCode) {
		requireAllowed(userId);
		Long partyId = partyService.getIdByInviteCode(inviteCode);
		requireTier(partyId, tierCode);
		admit(partyId, userId, positionCode, tierCode, PartyMemberService.Via.INVITE);
		return get(partyId, userId);
	}

	/** 멤버가 나간다 (US6) */
	@Transactional
	public void leave(Long userId, Long partyId) {
		leaveParty(partyId, userId);
	}

	/** 방장이 멤버를 내보낸다 (US7-AC5, AC10) */
	@Transactional
	public void kick(Long hostUserId, Long partyId, Long targetUserId) {
		KickResult result = partyMemberService.kick(partyId, hostUserId, targetUserId);
		publishMessage(result.message());
		publishMembersChanged(partyId);
		eventPublisher.publishEvent(PartyRealtimeEvent.toUser("KICKED", partyId, Map.of("partyId", partyId),
			targetUserId).thenUnsubscribe(targetUserId));
		notificationApplication.notify(targetUserId, NotificationType.PARTY_KICKED,
			"「" + result.title() + "」 파티에서 내보내졌습니다", RECRUIT_LINK);
	}

	/** 내 파티 (US8) */
	@Transactional(readOnly = true)
	public MyPartyView myParty(Long userId) {
		requireAllowed(userId);
		MyPartySnapshot mine = partyService.getMine(userId);
		PartySummaryView party = null;
		if (mine.party() != null) {
			Long hostId = mine.party().party().hostUserId();
			party = summaryView(mine.party(), userService.displayNames(List.of(hostId)));
		}
		return new MyPartyView(party, mine.myRole(), mine.joinRequests());
	}

	/**
	 * 회원 탈퇴 처리 (FR-031, research R15). 속한 파티에서 나가고(나가기와 같은 처리) 대기 신청을 모두 취소한다.
	 * 신청 취소는 방장에게 개인 알림을 보내지 않는다. 탈퇴 트랜잭션이 커밋된 뒤 불리므로 새 트랜잭션에서 돈다.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void leaveByWithdrawal(Long userId) {
		partyMemberService.findPartyIdOf(userId).ifPresent(partyId -> leaveParty(partyId, userId));
		publishJoinRequestsChanged(partyJoinRequestService.cancelAllOf(userId));
	}

	/** 탈퇴했는데 멤버 행이나 대기 신청이 남은 회원 (정리기가 부른다, research R15) */
	@Transactional(readOnly = true)
	public List<Long> findWithdrawalLeftovers() {
		return partyMemberService.findWithdrawnUserIdsWithLeftovers();
	}

	/**
	 * 멤버가 되는 공통 처리 (참가, 승인, 초대 입장). 부르는 쪽의 트랜잭션 안에서 돈다.
	 * JOINED 안내와 멤버 변화, 함께 취소된 신청의 목록 변화는 커밋 뒤에 보낸다.
	 */
	void admit(Long partyId, Long userId, String positionCode, String tierCode, PartyMemberService.Via via) {
		AdmitResult result = partyMemberService.admit(partyId, userId, positionCode, tierCode, via);
		publishMessage(result.joined());
		publishMembersChanged(partyId);
		publishJoinRequestsChanged(result.cancelledPartyIds());
	}

	/**
	 * 관리자·운영자 계정이면 막는다 (FR-012, research.md R14).
	 *
	 * @throws com.moa.common.exception.ServiceException A103
	 */
	void requireAllowed(Long userId) {
		memberActivityPolicy.requirePartyAllowed(userId);
	}

	/** 들어오는 회원이 적은 티어가 그 파티 게임의 티어인지 본다 (FR-024) */
	void requireTier(Long partyId, String tierCode) {
		if (tierCode != null) {
			partyService.requireTier(tierCode, codes(partyService.getGameCode(partyId)));
		}
	}

	/**
	 * 그 게임으로 고를 수 있는 공통코드 (research.md R7). 그룹 이름은 &lt;게임&gt;_MODE·_POSITION·_TIER 규칙이다.
	 * 없거나 꺼진 그룹은 빈 목록이다 (004 FR-008).
	 */
	PartyCodeBook codes(String gameCode) {
		Set<String> enabledGroups = codeService.getGroups().stream()
			.filter(CodeGroupDto::enabled)
			.map(CodeGroupDto::groupCode)
			.collect(Collectors.toSet());
		String game = gameCode == null ? "" : gameCode.trim();
		Set<String> games = codeSet(enabledGroups, GAME_GROUP);
		boolean known = games.contains(game);
		Map<String, Integer> tiers = new LinkedHashMap<>();
		if (known) {
			enabledCodes(enabledGroups, game + "_TIER").forEach(code -> tiers.put(code.code(), code.sortOrder()));
		}
		return new PartyCodeBook(games, known ? codeSet(enabledGroups, game + "_MODE") : Set.of(),
			known ? codeSet(enabledGroups, game + "_POSITION") : Set.of(), tiers);
	}

	private Set<String> codeSet(Set<String> enabledGroups, String groupCode) {
		return enabledCodes(enabledGroups, groupCode).stream().map(CodeDto::code).collect(Collectors.toSet());
	}

	private List<CodeDto> enabledCodes(Set<String> enabledGroups, String groupCode) {
		return enabledGroups.contains(groupCode) ? codeService.getCodes(groupCode, false) : List.of();
	}

	/** 나가기 공통 처리 (스스로 나감, 탈퇴). 방장이 바뀌면 새 방장에게 알린다 (US6-AC6) */
	private void leaveParty(Long partyId, Long userId) {
		LeaveResult result = partyMemberService.leave(partyId, userId);
		List<PartyMessageDto> messages = result.messages();
		// 나간 회원의 파티 주소 구독은 LEFT 안내를 보낸 뒤 끊는다
		publishMessage(messages.getFirst(), userId);
		messages.stream().skip(1).forEach(this::publishMessage);
		if (result.dissolved()) {
			return;
		}
		publishMembersChanged(partyId);
		if (result.newHostUserId() != null) {
			notificationApplication.notify(result.newHostUserId(), NotificationType.PARTY_HOST_CHANGED,
				"「" + result.title() + "」 파티의 방장이 되었습니다", "/parties/" + partyId);
		}
	}

	/** 채팅에 메시지를 보낸다 (MESSAGE) */
	void publishMessage(PartyMessageDto message) {
		publishMessage(message, null);
	}

	/**
	 * @param unsubscribeUserId 보낸 뒤 파티 주소 구독을 끊을 회원. 없으면 null
	 */
	private void publishMessage(PartyMessageDto message, Long unsubscribeUserId) {
		Map<Long, String> names = userService.displayNames(PartyPayloads.userIdsOf(message));
		PartyRealtimeEvent event = PartyRealtimeEvent.toParty("MESSAGE", message.partyId(),
			PartyPayloads.message(message, names));
		eventPublisher.publishEvent(unsubscribeUserId == null ? event : event.thenUnsubscribe(unsubscribeUserId));
	}

	/** 멤버 목록·빈자리·방장이 바뀌었다 (MEMBERS_CHANGED, US9-AC7) */
	void publishMembersChanged(Long partyId) {
		PartyView view = view(partyService.getSnapshotForMembers(partyId));
		eventPublisher.publishEvent(
			PartyRealtimeEvent.toParty("MEMBERS_CHANGED", partyId, PartyPayloads.membersChanged(view)));
	}

	/** 파티 정보·참가 방식·상태가 바뀌었다 (PARTY_UPDATED, FR-028) */
	void publishPartyUpdated(Long partyId) {
		PartyView view = view(partyService.getSnapshotForMembers(partyId));
		eventPublisher.publishEvent(
			PartyRealtimeEvent.toParty("PARTY_UPDATED", partyId, PartyPayloads.partyUpdated(view)));
	}

	/** 그 파티들의 방장에게 신청 목록이 바뀌었다고 알린다 (JOIN_REQUESTS_CHANGED, US9-AC9) */
	void publishJoinRequestsChanged(Collection<Long> partyIds) {
		if (partyIds.isEmpty()) {
			return;
		}
		for (PendingRequestCount count : partyJoinRequestService.pendingCounts(partyIds)) {
			eventPublisher.publishEvent(PartyRealtimeEvent.toUser("JOIN_REQUESTS_CHANGED", count.partyId(),
				Map.of("pendingCount", count.pendingCount()), count.hostUserId()));
		}
	}

	/** 읽은 파티에 표시 이름을 붙인다 */
	PartyView view(PartySnapshot snapshot) {
		Set<Long> userIds = new LinkedHashSet<>();
		userIds.add(snapshot.party().hostUserId());
		snapshot.members().forEach(member -> userIds.add(member.userId()));
		Map<Long, String> names = userService.displayNames(userIds);
		List<PartyView.Member> members = snapshot.members().stream()
			.map(member -> member(member, names))
			.toList();
		Long hostId = snapshot.party().hostUserId();
		boolean member = switch (snapshot.relation()) {
			case MEMBER, HOST -> true;
			default -> false;
		};
		return new PartyView(snapshot.party(), new MemberRef(hostId, names.get(hostId)), snapshot.positions(), members,
			snapshot.relation(), snapshot.joinRequestId(), member);
	}

	private static PartySummaryView summaryView(PartySummarySnapshot summary, Map<Long, String> names) {
		Long hostId = summary.party().hostUserId();
		return new PartySummaryView(summary.party(), new MemberRef(hostId, names.get(hostId)), summary.memberCount(),
			summary.positions());
	}

	private static PartyView.Member member(PartyMemberDto member, Map<Long, String> names) {
		return new PartyView.Member(new MemberRef(member.userId(), names.get(member.userId())), member.role(),
			member.positionCode(), member.tierCode(), member.joinedAt());
	}
}
