package com.moa.api.dto.party;

/** 방장의 신청 목록 한 줄. 신청한 회원의 표시 이름을 붙였다 (US5-AC2) */
public record PartyJoinRequestView(PartyJoinRequestDto request, MemberRef user) {
}
