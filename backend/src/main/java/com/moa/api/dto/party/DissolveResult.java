package com.moa.api.dto.party;

import java.util.Set;

/**
 * 방장이 해산한 결과 (specs/008-party US7-AC8)
 *
 * @param recipients PARTY_DISSOLVED를 받을 회원. 방장을 뺀 멤버와 대기 중이던 신청자다 (FR-029)
 */
public record DissolveResult(Long partyId, String title, Set<Long> recipients) {
}
