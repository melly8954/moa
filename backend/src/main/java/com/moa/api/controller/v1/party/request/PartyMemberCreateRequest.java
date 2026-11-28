package com.moa.api.controller.v1.party.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** 이름이 엔터티 PartyJoinRequest와 겹치지 않게 PartyMemberCreateRequest로 한다 (tasks.md T021) */
@Schema(description = "참가·초대 입장 (specs/008-party US3, US4, US10). 조건이 없는 파티면 비워 보낸다")
public record PartyMemberCreateRequest(
	@Schema(description = "고른 빈 포지션. 포지션 조건이 있으면 필요하다") @Size(max = 50) String positionCode,
	@Schema(description = "내 티어 (선택). 범위 밖이어도 막지 않는다") @Size(max = 50) String tierCode) {
}
