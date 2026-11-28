package com.moa.api.controller.v1.party.response;

import com.moa.api.dto.party.PartyPositionDto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "포지션 자리와 차지한 수")
public record PartyPositionResponse(
	@Schema(description = "포지션 코드") String positionCode,
	@Schema(description = "자리 수 (방장 자리 포함)") int slots,
	@Schema(description = "그 포지션의 멤버 수") int filled) {

	public static PartyPositionResponse from(PartyPositionDto dto) {
		return new PartyPositionResponse(dto.positionCode(), dto.slots(), dto.filled());
	}
}
