package com.moa.api.dto.party;

/** 한 파티의 대기 신청 수와 방장 (JOIN_REQUESTS_CHANGED, contracts/party-realtime.md) */
public record PendingRequestCount(Long partyId, Long hostUserId, int pendingCount) {
}
