package com.moa.api.dto.party;

import java.util.List;

/**
 * 메시지 한 묶음 (research.md R5). 오래된 것부터다
 *
 * @param hasMore 이 방향으로 더 있는지
 */
public record PartyMessagePage(List<PartyMessageDto> items, boolean hasMore) {
}
