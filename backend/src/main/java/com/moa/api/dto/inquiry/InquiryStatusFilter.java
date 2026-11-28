package com.moa.api.dto.inquiry;

/**
 * 문의 관리 목록의 상태 거르기 (specs/017-customer-center research R6). 보내지 않으면 답변 대기다.
 */
public enum InquiryStatusFilter {
	WAITING,
	ANSWERED,
	ALL
}
