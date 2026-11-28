package com.moa.api.controller.v1.party.response;

import java.util.List;

import com.moa.api.dto.party.PartyPositionDto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "빈자리가 남은 포지션")
public record PartyOpenPositionResponse(
	@Schema(description = "포지션 코드") String positionCode,
	@Schema(description = "남은 자리 수") int remaining) {

	/** 남은 자리가 있는 포지션만 */
	public static List<PartyOpenPositionResponse> of(List<PartyPositionDto> positions) {
		return positions.stream()
			.filter(position -> position.remaining() > 0)
			.map(position -> new PartyOpenPositionResponse(position.positionCode(), position.remaining()))
			.toList();
	}
}
