package com.moa.api.dto.party;

/**
 * 참가 신청 상태 (specs/008-party data-model.md tb_party_join_request). 엔터티와 API가 함께 쓰는 값이라 dto에 둔다
 */
public enum JoinRequestStatus {
	/** 대기 중 */
	PENDING,
	/** 승인됨 */
	APPROVED,
	/** 거절됨 */
	REJECTED,
	/** 취소됨 (본인 취소, 다른 파티에 들어감, 참가 방식 바뀜, 탈퇴) */
	CANCELLED
}
