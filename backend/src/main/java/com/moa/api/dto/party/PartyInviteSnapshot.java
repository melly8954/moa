package com.moa.api.dto.party;

/**
 * 초대 입장 화면 (contracts/party-api.md InvitePreview)를 읽은 그대로
 *
 * @param reasonCode 지금 들어올 수 없는 이유의 오류 코드 (B101 등). 들어올 수 있으면 null
 */
public record PartyInviteSnapshot(PartySnapshot snapshot, String reasonCode) {
}
