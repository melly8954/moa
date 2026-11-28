package com.moa.api.dto.party;

import java.time.LocalDateTime;

import com.moa.api.entity.party.Party;

/** 파티 한 행 (specs/008-party data-model.md tb_party) */
public record PartyDto(Long id, String gameCode, String modeCode, String tierMinCode, String tierMaxCode, String title,
	String description, int capacity, JoinPolicy joinPolicy, PartyStatus status, boolean closedByHost, Long hostUserId,
	boolean positionEnabled, String inviteCode, LocalDateTime createdAt) {

	public static PartyDto from(Party party) {
		return new PartyDto(party.getId(), party.getGameCode(), party.getModeCode(), party.getTierMinCode(),
			party.getTierMaxCode(), party.getTitle(), party.getDescription(), party.getCapacity(),
			party.getJoinPolicy(), party.getStatus(), party.isClosedByHost(), party.getHostUserId(),
			party.isPositionEnabled(), party.getInviteCode(), party.getCreatedAt());
	}
}
