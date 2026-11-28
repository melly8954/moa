package com.moa.api.application.v1.party.command;

import com.moa.api.dto.party.JoinPolicy;
import com.moa.api.dto.party.PartyForm;

/**
 * 파티 만들기 (specs/008-party US1, US10)
 *
 * @param form 방장이 정한 칸 (검증 전)
 * @param joinPolicy 참가 방식
 */
public record CreatePartyCommand(PartyForm form, JoinPolicy joinPolicy) {
}
