package com.moa.api.controller.v1.party.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "승인 파티에 참가 신청 (specs/008-party US5). 조건이 없는 파티면 비워 보낸다")
public record PartyJoinRequestCreateRequest(
	@Schema(description = "고른 빈 포지션. 포지션 조건이 있으면 필요하다") @Size(max = 50) String positionCode,
	@Schema(description = "내 티어 (선택)") @Size(max = 50) String tierCode) {
}
