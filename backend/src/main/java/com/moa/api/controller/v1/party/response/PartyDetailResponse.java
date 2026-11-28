package com.moa.api.controller.v1.party.response;

import java.time.OffsetDateTime;
import java.util.List;

import com.moa.api.dto.party.JoinPolicy;
import com.moa.api.dto.party.PartyDto;
import com.moa.api.dto.party.PartyStatus;
import com.moa.api.dto.party.PartyView;
import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

/** 파티 화면 (contracts/party-api.md PartyDetail) */
@Schema(description = "파티 상세")
public record PartyDetailResponse(
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
	@Schema(description = "소개") String description,
	@Schema(description = "모집 상태") PartyStatus status,
	@Schema(description = "방장이 직접 마감했는지") boolean closedByHost,
	@Schema(description = "포지션 조건을 걸었는지") boolean positionEnabled,
	@Schema(description = "포지션 자리와 차지한 수") List<PartyPositionResponse> positions,
	@Schema(description = "멤버. 들어온 순서") List<PartyMemberResponse> members,
	@Schema(description = "보는 회원과의 관계") PartyMeResponse me,
	@Schema(description = "초대 코드와 링크. 멤버에게만 있다") PartyInviteResponse invite) {

	public static PartyDetailResponse from(PartyView view) {
		PartyDto party = view.party();
		return new PartyDetailResponse(party.id(), party.gameCode(), party.modeCode(), party.title(),
			party.joinPolicy(), view.memberCount(), party.capacity(), PartyOpenPositionResponse.of(view.positions()),
			party.tierMinCode(), party.tierMaxCode(), PartyUserResponse.from(view.host()),
			ApiTimes.of(party.createdAt()), party.description(), party.status(), party.closedByHost(),
			party.positionEnabled(), view.positions().stream().map(PartyPositionResponse::from).toList(),
			view.members().stream().map(PartyMemberResponse::from).toList(),
			new PartyMeResponse(view.relation(), view.joinRequestId()),
			view.inviteVisible() ? PartyInviteResponse.from(party) : null);
	}
}
