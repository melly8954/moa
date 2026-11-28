package com.moa.api.dto.inquiry;

/**
 * 문의 상태 (specs/017-customer-center FR-008). 칸에 두지 않고 답변 시각으로 정한다 (research R3).
 * 엔터티와 API가 함께 쓰는 값이라 dto에 둔다.
 */
public enum InquiryStatus {
	/** 답변 대기 */
	WAITING,
	/** 답변 완료 */
	ANSWERED
}
