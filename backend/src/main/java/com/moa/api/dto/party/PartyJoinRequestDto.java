package com.moa.api.dto.party;

import java.time.LocalDateTime;

import com.moa.api.entity.party.PartyJoinRequest;

/** 참가 신청 (specs/008-party US5) */
public record PartyJoinRequestDto(Long id, Long partyId, Long userId, String positionCode, String tierCode,
	JoinRequestStatus status, LocalDateTime appliedAt) {

	public static PartyJoinRequestDto from(PartyJoinRequest request) {
		return new PartyJoinRequestDto(request.getId(), request.getPartyId(), request.getUserId(),
			request.getPositionCode(), request.getTierCode(), request.getStatus(), request.getCreatedAt());
	}
}
