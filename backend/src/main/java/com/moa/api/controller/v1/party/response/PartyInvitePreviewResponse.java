package com.moa.api.controller.v1.party.response;

import java.util.List;

import com.moa.api.dto.party.PartyDto;
import com.moa.api.dto.party.PartyInviteView;
import com.moa.api.dto.party.PartyView;

import io.swagger.v3.oas.annotations.media.Schema;

/** 초대 입장 화면 (contracts/party-api.md InvitePreview) */
@Schema(description = "초대 미리보기")
public record PartyInvitePreviewResponse(
	@Schema(description = "파티 ID") Long id,
	@Schema(description = "게임 코드") String gameCode,
	@Schema(description = "모드 코드") String modeCode,
	@Schema(description = "제목") String title,
	@Schema(description = "방장") PartyUserResponse host,
	@Schema(description = "현재 인원") int memberCount,
	@Schema(description = "모집 인원 (방장 포함)") int capacity,
	@Schema(description = "포지션 자리와 차지한 수") List<PartyPositionResponse> positions,
	@Schema(description = "보는 회원과의 관계") PartyMeResponse me,
	@Schema(description = "지금 들어올 수 있는지") boolean joinable,
	@Schema(description = "들어올 수 없는 이유의 오류 코드 (B101, B102, B103, B104). 들어올 수 있으면 null") String reasonCode) {

	public static PartyInvitePreviewResponse from(PartyInviteView invite) {
		PartyView view = invite.view();
		PartyDto party = view.party();
		return new PartyInvitePreviewResponse(party.id(), party.gameCode(), party.modeCode(), party.title(),
			PartyUserResponse.from(view.host()), view.memberCount(), party.capacity(),
			view.positions().stream().map(PartyPositionResponse::from).toList(),
			new PartyMeResponse(view.relation(), view.joinRequestId()), invite.joinable(), invite.reasonCode());
	}
}
