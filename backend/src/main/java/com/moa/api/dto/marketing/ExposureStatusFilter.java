package com.moa.api.dto.marketing;

/**
 * 배너·팝업 관리 목록의 상태 거르기 (specs/016-banner-popup FR-008, research R4).
 * 값을 보내지 않으면 끝난 것만 뺀다(기본). 끝난 것까지 모두 보려면 ALL.
 */
public enum ExposureStatusFilter {
	SCHEDULED,
	ACTIVE,
	ENDED,
	DISABLED,
	ALL
}
