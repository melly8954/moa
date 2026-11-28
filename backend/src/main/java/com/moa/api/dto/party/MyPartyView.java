package com.moa.api.dto.party;

import java.util.List;

/**
 * 내 파티 (contracts/party-api.md "내 파티")
 *
 * @param party 지금 속한 파티. 없으면 null
 * @param myRole 그 파티에서 내 역할. 없으면 null
 * @param joinRequests 대기 중인 신청. 신청순
 */
public record MyPartyView(PartySummaryView party, PartyRole myRole, List<MyJoinRequestDto> joinRequests) {
}
