package com.moa.api.dto.party;

/**
 * 초대 입장 화면 (contracts/party-api.md InvitePreview)
 *
 * @param reasonCode 지금 들어올 수 없는 이유의 오류 코드 (B101 등). 들어올 수 있으면 null
 */
public record PartyInviteView(PartyView view, String reasonCode) {

	public boolean joinable() {
		return reasonCode == null;
	}
}
