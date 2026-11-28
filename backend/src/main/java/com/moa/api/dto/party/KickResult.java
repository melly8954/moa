package com.moa.api.dto.party;

/**
 * 방장이 멤버를 내보낸 결과 (specs/008-party US7-AC5)
 *
 * @param message 남긴 KICKED 안내
 */
public record KickResult(Long partyId, String title, PartyMessageDto message) {
}
