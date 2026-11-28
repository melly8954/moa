package com.moa.api.application.v1.party;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.moa.api.dto.party.PartyDto;
import com.moa.api.dto.party.PartyMessageDto;
import com.moa.api.dto.party.PartyPositionDto;
import com.moa.api.dto.party.PartyView;
import com.moa.common.response.ApiTimes;

/**
 * 실시간 이벤트의 payload (contracts/party-realtime.md "이벤트"). REST 응답과 같은 이름·모양으로 만든다.
 */
final class PartyPayloads {

	private PartyPayloads() {
	}

	static List<Long> userIdsOf(PartyMessageDto message) {
		List<Long> ids = new ArrayList<>();
		if (message.senderUserId() != null) {
			ids.add(message.senderUserId());
		}
		if (message.subjectUserId() != null) {
			ids.add(message.subjectUserId());
		}
		return ids;
	}

	/** Message (contracts/party-api.md 채팅) */
	static Map<String, Object> message(PartyMessageDto message, Map<Long, String> names) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("id", message.id());
		payload.put("type", message.type().name());
		payload.put("sender", member(message.senderUserId(), names));
		payload.put("content", message.content());
		payload.put("systemEvent", message.systemEvent() == null ? null : message.systemEvent().name());
		payload.put("subject", member(message.subjectUserId(), names));
		payload.put("sentAt", String.valueOf(ApiTimes.of(message.sentAt())));
		return payload;
	}

	/** {members, positions, memberCount, capacity, status, hostUserId} */
	static Map<String, Object> membersChanged(PartyView view) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("members", view.members().stream().map(PartyPayloads::member).toList());
		payload.put("positions", view.positions().stream().map(PartyPayloads::position).toList());
		payload.put("memberCount", view.memberCount());
		payload.put("capacity", view.party().capacity());
		payload.put("status", view.party().status().name());
		payload.put("hostUserId", view.party().hostUserId());
		return payload;
	}

	/**
	 * PartyDetail의 파티 정보 부분 (PARTY_UPDATED). 파티 주소는 멤버만 구독하므로 초대 코드도 싣는다 (US4-AC1)
	 */
	static Map<String, Object> partyUpdated(PartyView view) {
		PartyDto party = view.party();
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("id", party.id());
		payload.put("gameCode", party.gameCode());
		payload.put("modeCode", party.modeCode());
		payload.put("title", party.title());
		payload.put("description", party.description());
		payload.put("joinPolicy", party.joinPolicy().name());
		payload.put("status", party.status().name());
		payload.put("closedByHost", party.closedByHost());
		payload.put("capacity", party.capacity());
		payload.put("memberCount", view.memberCount());
		payload.put("positionEnabled", party.positionEnabled());
		payload.put("tierMinCode", party.tierMinCode());
		payload.put("tierMaxCode", party.tierMaxCode());
		payload.put("positions", view.positions().stream().map(PartyPayloads::position).toList());
		payload.put("openPositions", view.positions().stream()
			.filter(position -> position.remaining() > 0)
			.map(position -> {
				Map<String, Object> open = new LinkedHashMap<>();
				open.put("positionCode", position.positionCode());
				open.put("remaining", position.remaining());
				return open;
			})
			.toList());
		Map<String, Object> invite = new LinkedHashMap<>();
		invite.put("code", party.inviteCode());
		invite.put("url", party.inviteUrl());
		payload.put("invite", invite);
		return payload;
	}

	private static Map<String, Object> member(PartyView.Member member) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("userId", member.user().userId());
		payload.put("displayName", member.user().displayName());
		payload.put("role", member.role().name());
		payload.put("positionCode", member.positionCode());
		payload.put("tierCode", member.tierCode());
		payload.put("joinedAt", String.valueOf(ApiTimes.of(member.joinedAt())));
		return payload;
	}

	private static Map<String, Object> position(PartyPositionDto position) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("positionCode", position.positionCode());
		payload.put("slots", position.slots());
		payload.put("filled", position.filled());
		return payload;
	}

	private static Map<String, Object> member(Long userId, Map<Long, String> names) {
		if (userId == null) {
			return null;
		}
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("userId", userId);
		payload.put("displayName", names.get(userId));
		return payload;
	}
}
