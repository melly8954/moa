package com.moa.api.service.party;

import java.util.List;
import java.util.Map;

import com.moa.api.entity.party.PartyMember;
import com.moa.api.entity.party.PartyPosition;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;

/**
 * 포지션 빈자리 확인 (specs/008-party FR-023). 참가·초대 입장(PartyMemberService)과 신청(PartyJoinRequestService)이 함께 쓴다.
 */
final class PartyPositions {

	private PartyPositions() {
	}

	/**
	 * 고른 포지션에 빈자리가 있는지 본다.
	 *
	 * @param members 지금 멤버 (파티 락 안에서 읽은 것)
	 * @throws ServiceException 고르지 않았으면 B106, 없는 포지션이거나 찼으면 B105(details.openPositions)
	 */
	static String requireOpen(List<PartyPosition> positions, List<PartyMember> members, String positionCode) {
		if (positionCode == null || positionCode.isBlank()) {
			throw new ServiceException(ErrorCode.PARTY_POSITION_REQUIRED);
		}
		List<String> open = open(positions, members);
		if (!open.contains(positionCode)) {
			throw ServiceException.withDetails(ErrorCode.PARTY_POSITION_FULL, Map.of("openPositions", open));
		}
		return positionCode;
	}

	/** 빈자리가 남은 포지션 */
	static List<String> open(List<PartyPosition> positions, List<PartyMember> members) {
		return positions.stream()
			.filter(position -> filled(members, position.getPositionCode()) < position.getSlots())
			.map(PartyPosition::getPositionCode)
			.toList();
	}

	static long filled(List<PartyMember> members, String positionCode) {
		return members.stream().filter(member -> positionCode.equals(member.getPositionCode())).count();
	}
}
