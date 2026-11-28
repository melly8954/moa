package com.moa.api.dto.party;

import com.moa.api.entity.party.PartyPosition;

/**
 * 포지션 자리와 차지한 수 (contracts/party-api.md PartyDetail.positions)
 *
 * @param filled 그 포지션의 멤버 수
 */
public record PartyPositionDto(String positionCode, int slots, int filled) {

	public static PartyPositionDto of(PartyPosition position, int filled) {
		return new PartyPositionDto(position.getPositionCode(), position.getSlots(), filled);
	}

	public int remaining() {
		return Math.max(0, slots - filled);
	}
}
