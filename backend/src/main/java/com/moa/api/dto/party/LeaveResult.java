package com.moa.api.dto.party;

import java.util.List;

/**
 * 멤버가 나간 결과 (specs/008-party US6)
 *
 * @param messages 남긴 안내 (LEFT, 방장이 바뀌었으면 HOST_CHANGED)
 * @param newHostUserId 방장을 이어받은 회원. 바뀌지 않았으면 null
 * @param dissolved 마지막 멤버가 나가 해산했는지
 */
public record LeaveResult(Long partyId, String title, List<PartyMessageDto> messages, Long newHostUserId,
	boolean dissolved) {
}
