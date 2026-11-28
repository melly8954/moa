package com.moa.api.application.v1.party.command;

import com.moa.api.dto.party.PartyForm;

/**
 * 파티 정보 고치기 (specs/008-party US7-AC1, US10-AC10). 참가 방식은 따로 바꾼다
 *
 * @param form 방장이 정한 칸 (검증 전)
 */
public record ModifyPartyCommand(PartyForm form) {
}
