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

	/** 초대 링크. 화면 경로이고, 앞의 주소(origin)는 화면이 붙인다 (spec.md 화면 "초대 입장 화면") */
	public String inviteUrl() {
		return "/invite/" + inviteCode;
	}
}
