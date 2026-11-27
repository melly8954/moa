package com.moa.api.dto.party;

/**
 * 파티 참가 제한 이유 (specs/008-party FR-013). 엔터티와 API가 함께 쓰는 값이라 dto에 둔다
 */
public enum BanReason {
	/** 내보내짐 */
	KICKED,
	/** 신청이 거절됨 */
	REJECTED
}
