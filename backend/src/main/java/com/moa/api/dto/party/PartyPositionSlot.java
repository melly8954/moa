package com.moa.api.dto.party;

/** 포지션 하나와 그 자리 수 (specs/008-party FR-022) */
public record PartyPositionSlot(String positionCode, Integer slots) {
}
