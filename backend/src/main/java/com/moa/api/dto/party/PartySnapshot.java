package com.moa.api.dto.party;

import java.util.List;

/**
 * 파티 한 개를 읽은 그대로 (이름을 붙이기 전). Application이 표시 이름을 붙여 PartyView로 바꾼다
 *
 * @param members 들어온 순서
 * @param relation 보는 회원과의 관계
 * @param joinRequestId 보는 회원의 대기 신청 ID (relation이 PENDING일 때만)
 */
public record PartySnapshot(PartyDto party, List<PartyPositionDto> positions, List<PartyMemberDto> members,
	PartyRelation relation, Long joinRequestId) {
}
