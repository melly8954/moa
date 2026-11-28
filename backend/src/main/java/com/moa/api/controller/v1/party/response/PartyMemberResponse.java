package com.moa.api.controller.v1.party.response;

import java.time.OffsetDateTime;

import com.moa.api.dto.party.PartyRole;
import com.moa.api.dto.party.PartyView;
import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "파티 멤버")
public record PartyMemberResponse(
	@Schema(description = "회원 ID") Long userId,
	@Schema(description = "표시 이름 (닉네임)") String displayName,
	@Schema(description = "역할") PartyRole role,
	@Schema(description = "차지한 포지션. 포지션 조건이 없으면 null") String positionCode,
	@Schema(description = "본인이 적은 티어 (선택)") String tierCode,
	@Schema(description = "들어온 시각") OffsetDateTime joinedAt) {

	public static PartyMemberResponse from(PartyView.Member member) {
		return new PartyMemberResponse(member.user().userId(), member.user().displayName(), member.role(),
			member.positionCode(), member.tierCode(), ApiTimes.of(member.joinedAt()));
	}
}
