package com.moa.api.dto.party;

/**
 * 파티 참가 방식 (specs/008-party FR-005). 엔터티와 API가 함께 쓰는 값이라 dto에 둔다
 */
public enum JoinPolicy {
	/** 공개: 바로 참가 */
	OPEN,
	/** 승인: 신청하고 방장이 받음 */
	APPROVAL,
	/** 비공개: 초대 코드로만, 모집 페이지에 안 보임 */
	PRIVATE
}
