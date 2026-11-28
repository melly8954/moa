package com.moa.api.dto.legal;

/**
 * 버전 상태. 칸이 아니라 오늘 날짜로 정한다 (specs/012-terms-privacy data-model "버전 상태", research R5).
 */
public enum VersionState {
	/** 지금 시행 중: 시행일이 오늘 이전인 것 중 가장 늦은 것 */
	CURRENT,
	/** 예전: 그 밖의 시행된 버전 */
	PAST,
	/** 예정: 시행일이 오늘 뒤 */
	UPCOMING
}
