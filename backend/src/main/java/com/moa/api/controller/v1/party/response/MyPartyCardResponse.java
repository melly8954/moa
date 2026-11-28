package com.moa.api.controller.v1.party.response;

import java.time.OffsetDateTime;
import java.util.List;

import com.moa.api.dto.party.JoinPolicy;
import com.moa.api.dto.party.PartyDto;
import com.moa.api.dto.party.PartyRole;
import com.moa.api.dto.party.PartySummaryView;
import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

/** 내가 속한 파티 카드 (PartySummary + myRole) */
@Schema(description = "내가 속한 파티")
public record MyPartyCardResponse(
	@Schema(description = "파티 ID") Long id,
	@Schema(description = "게임 코드") String gameCode,
	@Schema(description = "모드 코드") String modeCode,
	@Schema(description = "제목") String title,
	@Schema(description = "참가 방식") JoinPolicy joinPolicy,
	@Schema(description = "현재 인원") int memberCount,
	@Schema(description = "모집 인원 (방장 포함)") int capacity,
	@Schema(description = "빈 포지션") List<PartyOpenPositionResponse> openPositions,
	@Schema(description = "최소 티어 코드") String tierMinCode,
	@Schema(description = "최대 티어 코드") String tierMaxCode,
	@Schema(description = "방장") PartyUserResponse host,
	@Schema(description = "만든 시각") OffsetDateTime createdAt,
	@Schema(description = "내 역할") PartyRole myRole) {

	public static MyPartyCardResponse from(PartySummaryView view, PartyRole myRole) {
		PartyDto party = view.party();
		return new MyPartyCardResponse(party.id(), party.gameCode(), party.modeCode(), party.title(),
			party.joinPolicy(), view.memberCount(), party.capacity(), PartyOpenPositionResponse.of(view.positions()),
			party.tierMinCode(), party.tierMaxCode(), PartyUserResponse.from(view.host()),
			ApiTimes.of(party.createdAt()), myRole);
	}
}
