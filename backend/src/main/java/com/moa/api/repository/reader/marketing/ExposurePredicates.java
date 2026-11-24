package com.moa.api.repository.reader.marketing;

import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.marketing.ExposureStatusFilter;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.DateTimePath;

/**
 * 배너·팝업 목록의 상태 거르기 조건 (specs/016-banner-popup research R3·R4). ExposureStatus.of와 같은 규칙을 질의로 쓴다.
 */
public final class ExposurePredicates {

	private ExposurePredicates() {
	}

	/**
	 * @param filter 비면 끝난 것만 뺀다. ALL이면 조건이 없다(null)
	 */
	public static @Nullable Predicate of(@Nullable ExposureStatusFilter filter, BooleanPath enabled,
		DateTimePath<LocalDateTime> startsAt, DateTimePath<LocalDateTime> endsAt, LocalDateTime now) {
		if (filter == null) {
			return enabled.isFalse().or(endsAt.isNull()).or(endsAt.after(now));
		}
		return switch (filter) {
			case DISABLED -> enabled.isFalse();
			case SCHEDULED -> enabled.isTrue().and(startsAt.after(now));
			case ENDED -> enabled.isTrue().and(endsAt.isNotNull()).and(endsAt.loe(now));
			case ACTIVE -> enabled.isTrue().and(startsAt.loe(now)).and(endsAt.isNull().or(endsAt.after(now)));
			case ALL -> null;
		};
	}
}
