package com.moa.api.dto.party;

import java.util.List;

/**
 * 모집 페이지 카드 한 장 (contracts/party-api.md PartySummary)
 *
 * @param positions 포지션 자리와 차지한 수. 남은 자리는 remaining()
 */
public record PartySummaryView(PartyDto party, MemberRef host, int memberCount, List<PartyPositionDto> positions) {
}
