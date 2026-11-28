package com.moa.api.dto.party;

/** 내 대기 신청과 그 파티 (specs/008-party US8) */
public record MyJoinRequestDto(PartyJoinRequestDto request, PartyDto party) {
}
