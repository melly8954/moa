package com.moa.api.application.v1.party;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
