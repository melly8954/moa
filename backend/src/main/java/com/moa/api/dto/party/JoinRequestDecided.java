package com.moa.api.dto.party;

/** 방장이 승인·거절한 신청과 파티 제목 (specs/008-party US5) */
public record JoinRequestDecided(PartyJoinRequestDto request, String title) {
}
