package com.moa.api.dto.party;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 파티 화면 한 장에 필요한 것 (contracts/party-api.md PartyDetail)
 *
 * @param members 들어온 순서
 * @param joinRequestId 보는 회원의 대기 신청 ID (relation이 PENDING일 때만)
 * @param inviteVisible 초대 코드를 보일지 (멤버에게만, US4-AC1)
 */
public record PartyView(PartyDto party, MemberRef host, List<PartyPositionDto> positions, List<Member> members,
	PartyRelation relation, Long joinRequestId, boolean inviteVisible) {

	public int memberCount() {
		return members.size();
	}

	/** 표시 이름을 붙인 멤버 */
	public record Member(MemberRef user, PartyRole role, String positionCode, String tierCode,
		LocalDateTime joinedAt) {
	}
}
