package com.moa.api.application.v1.purgelog.query;

import java.time.LocalDate;

import com.moa.common.purgelog.PurgeTarget;

/**
 * 파기 기록 검색 조건. 모두 선택이다.
 *
 * @param target 대상 종류 (null: 전체)
 * @param from 시작일 (포함)
 * @param to 종료일 (포함, 그날 끝까지)
 */
public record PurgeLogQuery(PurgeTarget target, LocalDate from, LocalDate to) {
}
