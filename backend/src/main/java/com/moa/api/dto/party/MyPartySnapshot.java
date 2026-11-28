package com.moa.api.dto.party;

import java.util.List;

/**
 * 내 파티를 읽은 그대로 (specs/008-party US8)
 *
 * @param party 지금 속한 파티. 없으면 null
 * @param myRole 그 파티에서 내 역할. 없으면 null
 * @param joinRequests 대기 중인 신청. 신청순
 */
public record MyPartySnapshot(PartySummarySnapshot party, PartyRole myRole, List<MyJoinRequestDto> joinRequests) {
}
