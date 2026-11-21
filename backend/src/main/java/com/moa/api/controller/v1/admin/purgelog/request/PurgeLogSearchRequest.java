package com.moa.api.controller.v1.admin.purgelog.request;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.moa.api.application.v1.purgelog.query.PurgeLogQuery;
import com.moa.common.purgelog.PurgeTarget;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 파기 이력 검색 조건 (쿼리 파라미터). 모두 선택이고 함께 주면 AND.
 */
@Schema(description = "파기 이력 검색 조건")
public record PurgeLogSearchRequest(
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "대상 종류. 없으면 전체") PurgeTarget target,
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "시작일 (포함)",
		example = "2026-11-01") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "종료일 (포함, 그날 끝까지). from보다 이르면 V001",
		example = "2026-11-30") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

	public PurgeLogQuery toQuery() {
		return new PurgeLogQuery(target, from, to);
	}
}
