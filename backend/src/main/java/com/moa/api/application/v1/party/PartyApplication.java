package com.moa.api.application.v1.party;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.dto.party.MemberRef;
import com.moa.api.dto.party.PartyMemberDto;
import com.moa.api.dto.party.PartyMessageDto;
import com.moa.api.dto.party.PartySnapshot;
import com.moa.api.dto.party.PartyView;
import com.moa.api.service.party.PartyMemberService;
import com.moa.api.service.party.PartyService;
import com.moa.api.service.user.MemberActivityPolicy;
import com.moa.api.service.user.UserService;

import lombok.RequiredArgsConstructor;

/**
 * 파티 (specs/008-party). 트랜잭션, 관리자 계정 막기, 표시 이름 붙이기, 실시간 이벤트를 맡는다.
 * 파티 규칙은 Service에 있다 (research.md R10).
 */
@Component
@RequiredArgsConstructor
public class PartyApplication {

	private final PartyService partyService;
	private final PartyMemberService partyMemberService;
	private final UserService userService;
	private final MemberActivityPolicy memberActivityPolicy;
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

	/**
	 * 멤버가 되는 공통 처리 (참가, 승인, 초대 입장). 부르는 쪽의 트랜잭션 안에서 돈다.
	 * JOINED 안내와 멤버 변화는 커밋 뒤에 보낸다.
	 */
	void admit(Long partyId, Long userId, String positionCode, String tierCode, PartyMemberService.Via via) {
		PartyMessageDto joined = partyMemberService.admit(partyId, userId, positionCode, tierCode, via);
		publishMessage(joined);
		publishMembersChanged(partyId);
	}

	/**
	 * 관리자·운영자 계정이면 막는다 (FR-012, research.md R14).
	 *
	 * @throws com.moa.common.exception.ServiceException A103
	 */
	void requireAllowed(Long userId) {
		memberActivityPolicy.requirePartyAllowed(userId);
	}

	/** 채팅에 메시지를 보낸다 (MESSAGE) */
	void publishMessage(PartyMessageDto message) {
		Map<Long, String> names = userService.displayNames(PartyPayloads.userIdsOf(message));
		eventPublisher.publishEvent(
			PartyRealtimeEvent.toParty("MESSAGE", message.partyId(), PartyPayloads.message(message, names)));
	}

	/** 멤버 목록·빈자리·방장이 바뀌었다 (MEMBERS_CHANGED, US9-AC7) */
	void publishMembersChanged(Long partyId) {
		PartyView view = view(partyService.getSnapshot(partyId, null));
		eventPublisher.publishEvent(
			PartyRealtimeEvent.toParty("MEMBERS_CHANGED", partyId, PartyPayloads.membersChanged(view)));
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

	private static PartyView.Member member(PartyMemberDto member, Map<Long, String> names) {
		return new PartyView.Member(new MemberRef(member.userId(), names.get(member.userId())), member.role(),
			member.positionCode(), member.tierCode(), member.joinedAt());
	}
}
