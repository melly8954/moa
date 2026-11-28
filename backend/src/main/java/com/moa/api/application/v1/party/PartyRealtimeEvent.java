package com.moa.api.application.v1.party;

/**
 * 파티 화면을 바꾸는 실시간 이벤트 (contracts/party-realtime.md "이벤트"). 커밋 뒤 PartyRealtimeNotifier가 보낸다.
 *
 * @param type MESSAGE, MEMBERS_CHANGED, PARTY_UPDATED, PARTY_DISSOLVED, KICKED, JOIN_REQUESTS_CHANGED
 * @param payload 화면에 갈 값 (응답과 같은 모양의 Map)
 * @param toUserId 개인 주소(/user/queue/parties)로 보낼 회원. null이면 파티 주소(/topic/parties/{id})
 * @param unsubscribeUserId 보낸 뒤 파티 주소 구독을 끊을 회원 (나감, 내보내짐). 없으면 null
 */
public record PartyRealtimeEvent(String type, Long partyId, Object payload, Long toUserId, Long unsubscribeUserId) {

	public static PartyRealtimeEvent toParty(String type, Long partyId, Object payload) {
		return new PartyRealtimeEvent(type, partyId, payload, null, null);
	}

	public static PartyRealtimeEvent toUser(String type, Long partyId, Object payload, Long userId) {
		return new PartyRealtimeEvent(type, partyId, payload, userId, null);
	}

	/** 이 이벤트를 보낸 뒤 그 회원의 파티 구독을 끊는다 */
	public PartyRealtimeEvent thenUnsubscribe(Long userId) {
		return new PartyRealtimeEvent(type, partyId, payload, toUserId, userId);
	}
}
