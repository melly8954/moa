package com.moa.api.dto.party;

/** 승인 신청을 받은 결과 (specs/008-party US5-AC1) */
public record JoinRequestApplied(Long joinRequestId, Long partyId, Long hostUserId, String title) {
}
