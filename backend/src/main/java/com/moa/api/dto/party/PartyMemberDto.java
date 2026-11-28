package com.moa.api.dto.party;

import java.time.LocalDateTime;

import com.moa.api.entity.party.PartyMember;

/** 파티 멤버. 표시 이름은 Application이 MemberRef로 붙인다 */
public record PartyMemberDto(Long userId, PartyRole role, String positionCode, String tierCode,
	LocalDateTime joinedAt) {

	public static PartyMemberDto from(PartyMember member) {
		return new PartyMemberDto(member.getUserId(), member.getRole(), member.getPositionCode(),
			member.getTierCode(), member.getJoinedAt());
	}
}
