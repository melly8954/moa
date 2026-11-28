package com.moa.api.dto.party;

/**
 * 파티 모집 상태 (specs/008-party data-model.md "상태 전이"). 엔터티와 API가 함께 쓰는 값이라 dto에 둔다
 */
public enum PartyStatus {
	/** 모집 중 */
	RECRUITING,
	/** 마감 (정원 참 또는 방장 마감) */
	CLOSED
}
