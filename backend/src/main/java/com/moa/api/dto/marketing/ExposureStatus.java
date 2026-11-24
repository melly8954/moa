package com.moa.api.dto.marketing;

import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;

/**
 * 배너·팝업의 상태 (specs/016-banner-popup FR-006). 칸에 저장하지 않고 켜짐과 노출 기간으로 읽을 때 정한다 (research R3).
 * 이번에는 사용자 화면에 보이지 않으므로 ACTIVE는 "노출 기간 안이고 켜져 있다"는 뜻이다.
 */
public enum ExposureStatus {
	/** 예정: 켜져 있고 노출 시작 전 */
	SCHEDULED,
	/** 노출 중: 켜져 있고 노출 기간 안 */
	ACTIVE,
	/** 끝남: 켜져 있고 노출 끝 일시가 지남 */
	ENDED,
	/** 꺼짐 */
	DISABLED;

	public static ExposureStatus of(boolean enabled, LocalDateTime startsAt, @Nullable LocalDateTime endsAt,
		LocalDateTime now) {
		if (!enabled) {
			return DISABLED;
		}
		if (now.isBefore(startsAt)) {
			return SCHEDULED;
		}
		if (endsAt != null && !endsAt.isAfter(now)) {
			return ENDED;
		}
		return ACTIVE;
	}
}
