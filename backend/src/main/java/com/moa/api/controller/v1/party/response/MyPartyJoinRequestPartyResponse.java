package com.moa.api.controller.v1.party.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "신청한 파티 요약")
public record MyPartyJoinRequestPartyResponse(
	@Schema(description = "파티 ID") Long id,
	@Schema(description = "제목") String title,
	@Schema(description = "게임 코드") String gameCode) {
}
