package com.moa.api.controller.v1.party.request;

import com.moa.api.dto.party.JoinPolicy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "참가 방식 바꾸기 (specs/008-party US7-AC2~AC4)")
public record PartyJoinPolicyRequest(@Schema(description = "참가 방식") @NotNull JoinPolicy joinPolicy) {
}
