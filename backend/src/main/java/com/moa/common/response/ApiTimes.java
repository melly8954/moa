package com.moa.common.response;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.jspecify.annotations.Nullable;

/**
 * 응답 시각. API는 시각을 시간대(오프셋)를 붙여 내보낸다 (CONTRACT.md 4절, 예: 2026-11-02T14:30:00+09:00).
 * 엔터티의 LocalDateTime은 서버 시간대로 저장된 값이므로 그 시간대의 오프셋을 붙인다.
 * 응답 record에는 LocalDateTime을 쓰지 않는다 (ArchitectureTest가 막는다). 날짜만 있는 값은 LocalDate를 그대로 쓴다.
 */
public final class ApiTimes {

	private ApiTimes() {
		// 정적 메서드만 쓴다
	}

	public static @Nullable OffsetDateTime of(@Nullable LocalDateTime time) {
		return time == null ? null : time.atZone(ZoneId.systemDefault()).toOffsetDateTime();
	}
}
