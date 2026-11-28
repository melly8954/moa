package com.moa.api.controller.v1.party.request;

import com.moa.api.dto.party.PartyPositionSlot;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "모집할 포지션과 자리 수")
public record PartyPositionRequest(
	@Schema(description = "포지션 코드 (<게임>_POSITION)", example = "JUNGLE") @NotBlank String positionCode,
	@Schema(description = "자리 수. 롤은 늘 1", example = "1") @NotNull @Min(1) @Max(10) Integer slots) {

	public PartyPositionSlot toSlot() {
		return new PartyPositionSlot(positionCode, slots);
	}
}
