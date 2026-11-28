package com.moa.api.dto.party;

import java.util.List;

/** 이름을 붙인 메시지 한 묶음 (research.md R5) */
public record PartyMessagePageView(List<PartyMessageView> items, boolean hasMore) {
}
