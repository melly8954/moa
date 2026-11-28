package com.moa.api.controller.v1.party.response;

import java.time.OffsetDateTime;

import com.moa.api.dto.party.MyJoinRequestDto;
import com.moa.api.dto.party.PartyDto;
import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 대기 신청")
public record MyPartyJoinRequestResponse(
	@Schema(description = "신청 ID") Long joinRequestId,
	@Schema(description = "신청한 파티") MyPartyJoinRequestPartyResponse party,
	@Schema(description = "고른 포지션") String positionCode,
	@Schema(description = "신청 시각") OffsetDateTime appliedAt) {

	public static MyPartyJoinRequestResponse from(MyJoinRequestDto dto) {
		PartyDto party = dto.party();
		return new MyPartyJoinRequestResponse(dto.request().id(),
			new MyPartyJoinRequestPartyResponse(party.id(), party.title(), party.gameCode()),
			dto.request().positionCode(), ApiTimes.of(dto.request().appliedAt()));
	}
}
