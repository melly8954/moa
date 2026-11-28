package com.moa.api.dto.party;

import java.util.List;

/** 모집 페이지 카드 한 장을 읽은 그대로 (이름을 붙이기 전) */
public record PartySummarySnapshot(PartyDto party, int memberCount, List<PartyPositionDto> positions) {
}
