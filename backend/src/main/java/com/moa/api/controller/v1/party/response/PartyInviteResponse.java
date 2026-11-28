package com.moa.api.controller.v1.party.response;

import com.moa.api.dto.party.PartyDto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "초대 코드와 링크 (specs/008-party US4-AC1). 멤버에게만 보인다")
public record PartyInviteResponse(
	@Schema(description = "초대 코드 8자", example = "K7Q2M9XA") String code,
	@Schema(description = "초대 링크 (화면 경로). 앞의 주소는 화면이 붙인다", example = "/invite/K7Q2M9XA") String url) {

	public static PartyInviteResponse from(PartyDto party) {
		return new PartyInviteResponse(party.inviteCode(), party.inviteUrl());
	}
}
