package com.moa.api.controller.v1.party.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "받은 신청")
public record PartyJoinRequestCreateResponse(@Schema(description = "신청 ID") Long joinRequestId) {
}
