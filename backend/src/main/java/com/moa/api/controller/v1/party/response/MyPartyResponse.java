package com.moa.api.controller.v1.party.response;

import java.util.List;

import com.moa.api.dto.party.MyPartyView;

import io.swagger.v3.oas.annotations.media.Schema;

/** 내 파티 (contracts/party-api.md "내 파티", specs/008-party US8) */
@Schema(description = "내 파티")
public record MyPartyResponse(
	@Schema(description = "지금 속한 파티. 없으면 null") MyPartyCardResponse party,
	@Schema(description = "대기 중인 신청. 신청순") List<MyPartyJoinRequestResponse> joinRequests) {

	public static MyPartyResponse from(MyPartyView view) {
		return new MyPartyResponse(view.party() == null ? null : MyPartyCardResponse.from(view.party(), view.myRole()),
			view.joinRequests().stream().map(MyPartyJoinRequestResponse::from).toList());
	}
}
