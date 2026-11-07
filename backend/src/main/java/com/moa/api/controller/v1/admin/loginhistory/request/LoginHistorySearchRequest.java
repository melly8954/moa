package com.moa.api.controller.v1.admin.loginhistory.request;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.moa.api.application.v1.loginhistory.query.LoginHistoryQuery;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 접속 이력 검색 조건 (쿼리 파라미터). 모두 선택이고 함께 주면 AND.
 */
@Schema(description = "접속 이력 검색 조건")
public record LoginHistorySearchRequest(
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED,
		description = "사용자 ID. 그 사용자의 성공한 로그인만 (실패 이력에는 사용자 ID가 없다)") Long userId,
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "입력한 아이디. 정확히 일치",
		example = "kim") @Size(max = 50) String loginId,
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED,
		description = "true면 성공만, false면 실패만. 없으면 전체") Boolean success,
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "시작일 (포함)",
		example = "2026-11-01") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "종료일 (포함, 그날 끝까지). from보다 이르면 V001",
		example = "2026-11-30") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

	public LoginHistoryQuery toQuery() {
		return new LoginHistoryQuery(userId, loginId, success, from, to);
	}
}
