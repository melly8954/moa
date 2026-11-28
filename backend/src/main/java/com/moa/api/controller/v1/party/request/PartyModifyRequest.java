package com.moa.api.controller.v1.party.request;

import java.util.List;

import com.moa.api.application.v1.party.command.ModifyPartyCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "파티 정보 고치기 (specs/008-party US7-AC1, US10-AC10). 모든 칸을 보낸 값으로 바꾼다")
public record PartyModifyRequest(
	@Schema(description = "게임 코드 (GAME)", example = "LOL") @NotBlank String gameCode,
	@Schema(description = "모드 코드 (<게임>_MODE)") String modeCode,
	@Schema(description = "최소 티어 코드 (<게임>_TIER)") String tierMinCode,
	@Schema(description = "최대 티어 코드 (<게임>_TIER)") String tierMaxCode,
	@Schema(description = "제목 1~50자") @NotBlank @Size(min = 1, max = 50) String title,
	@Schema(description = "소개 0~1000자") @Size(max = 1000) String description,
	@Schema(description = "모집 인원 (방장 포함) 2~10. 포지션 조건이 있으면 쓰지 않고 자리의 합으로 정한다") @Min(2) @Max(10) Integer capacity,
	@Schema(description = "모집할 포지션. 비우면 포지션 조건이 없다") @Valid @Size(max = 10) List<PartyPositionRequest> positions,
	@Schema(description = "방장 포지션. 포지션 조건이 있으면 필요하다") String hostPositionCode) {

	public ModifyPartyCommand toCommand() {
		return new ModifyPartyCommand(PartyRequests.form(gameCode, modeCode, tierMinCode, tierMaxCode, title,
			description, capacity, positions, hostPositionCode));
	}
}
