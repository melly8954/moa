package com.moa.api.service.party;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moa.api.dto.party.JoinPolicy;
import com.moa.api.dto.party.PartyDto;
import com.moa.api.dto.party.PartyMemberDto;
import com.moa.api.dto.party.PartyPositionDto;
import com.moa.api.dto.party.PartyRelation;
import com.moa.api.dto.party.PartySnapshot;
import com.moa.api.entity.party.Party;
import com.moa.api.entity.party.PartyJoinRequest;
import com.moa.api.entity.party.PartyMember;
import com.moa.api.entity.party.PartyPosition;
import com.moa.api.repository.reader.party.PartyReader;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;

import lombok.RequiredArgsConstructor;

/**
 * 파티 조회 (specs/008-party). 만들기·고치기는 각 스토리에서 더한다.
 */
@Service
@RequiredArgsConstructor
public class PartyService {

	private final PartyReader partyReader;

	/**
	 * 파티 화면 한 장. 비공개 파티는 멤버에게만 보인다 (FR-014).
	 *
	 * @param viewerUserId 로그인하지 않았으면 null
	 * @throws ServiceException 없거나 해산했거나, 멤버가 아닌 회원의 비공개 파티면 R001
	 */
	public PartySnapshot getSnapshot(Long partyId, Long viewerUserId) {
		Party party = partyReader.find(partyId).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
		List<PartyMember> members = partyReader.findMembers(partyId);
		boolean member = viewerUserId != null
			&& members.stream().anyMatch(m -> m.getUserId().equals(viewerUserId));
		if (party.getJoinPolicy() == JoinPolicy.PRIVATE && !member) {
			throw new ServiceException(ErrorCode.NOT_FOUND);
		}
		Long joinRequestId = null;
		PartyRelation relation = PartyRelation.NONE;
		if (member) {
			relation = party.isHost(viewerUserId) ? PartyRelation.HOST : PartyRelation.MEMBER;
		} else if (viewerUserId != null) {
			if (partyReader.isBanned(partyId, viewerUserId)) {
				relation = PartyRelation.BANNED;
			} else {
				joinRequestId = partyReader.findPendingRequest(partyId, viewerUserId)
					.map(PartyJoinRequest::getId)
					.orElse(null);
				relation = joinRequestId == null ? PartyRelation.NONE : PartyRelation.PENDING;
			}
		}
		return new PartySnapshot(PartyDto.from(party), positions(partyReader.findPositions(partyId), members),
			members.stream().map(PartyMemberDto::from).toList(), relation, joinRequestId);
	}

	/** 포지션마다 자리와 차지한 수 */
	static List<PartyPositionDto> positions(List<PartyPosition> positions, List<PartyMember> members) {
		return positions.stream()
			.map(position -> PartyPositionDto.of(position, (int)members.stream()
				.filter(m -> position.getPositionCode().equals(m.getPositionCode()))
				.count()))
			.toList();
	}
}
