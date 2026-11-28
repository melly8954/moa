package com.moa.api.controller.v1.party.response;

import com.moa.api.dto.party.PartyRelation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "보는 회원과 파티의 관계")
public record PartyMeResponse(
	@Schema(description = "관계. 로그인하지 않았으면 NONE") PartyRelation relation,
	@Schema(description = "내 대기 신청 ID. relation이 PENDING일 때만") Long joinRequestId) {
}
