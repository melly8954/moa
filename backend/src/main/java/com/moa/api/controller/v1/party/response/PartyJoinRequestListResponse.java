package com.moa.api.controller.v1.party.response;

import java.time.OffsetDateTime;

import com.moa.api.dto.party.PartyJoinRequestView;
import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "방장의 신청 목록 한 줄")
public record PartyJoinRequestListResponse(
	@Schema(description = "신청 ID") Long joinRequestId,
	@Schema(description = "신청한 회원") PartyUserResponse user,
	@Schema(description = "고른 포지션") String positionCode,
	@Schema(description = "적은 티어") String tierCode,
	@Schema(description = "신청 시각") OffsetDateTime appliedAt) {

	public static PartyJoinRequestListResponse from(PartyJoinRequestView view) {
		return new PartyJoinRequestListResponse(view.request().id(), PartyUserResponse.from(view.user()),
			view.request().positionCode(), view.request().tierCode(), ApiTimes.of(view.request().appliedAt()));
	}
}
