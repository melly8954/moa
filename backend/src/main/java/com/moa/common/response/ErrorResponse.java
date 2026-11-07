package com.moa.common.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.moa.common.exception.ErrorCode;
import com.moa.common.logging.TraceIdFilter;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 오류 응답. 형식은 psw-dev-kit CONTRACT.md 4절과 같다.
 */
@Schema(description = "오류 응답")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
	@Schema(description = "에러 코드", example = "V001") String code,
	@Schema(description = "기본 설명 문장. 로그·API 문서용이고 화면에 쓰지 않는다. 화면 문구는 code로 고른다") String message,
	@Schema(description = "발생 시각") OffsetDateTime timestamp,
	@Schema(description = "요청 경로") String path,
	@Schema(description = "요청 추적 ID. 서버 로그와 같은 값") @Nullable String traceId,
	@Schema(description = "필드별 검증 오류. V001일 때만 있다") @Nullable List<FieldError> errors,
	@Schema(description = "화면 안내에 끼울 값. 값이 필요한 오류만 있다 (문장이 아닌 데이터, 시각은 ISO-8601)",
		example = "{\"lockedUntil\": \"2026-10-10T14:30:00+09:00\"}") @Nullable Map<String, Object> details) {

	public static ErrorResponse of(ErrorCode errorCode, String message, String path, List<FieldError> errors,
		Map<String, Object> details) {
		return new ErrorResponse(errorCode.getCode(), message, OffsetDateTime.now(), path,
			TraceIdFilter.currentTraceId(),
			errors, details);
	}

	public static ErrorResponse of(ErrorCode errorCode, String message, String path, List<FieldError> errors) {
		return of(errorCode, message, path, errors, Map.of());
	}

	public static ErrorResponse of(ErrorCode errorCode, String path) {
		return of(errorCode, errorCode.getMessage(), path, List.of());
	}

	@Schema(description = "필드별 검증 오류")
	public record FieldError(
		@Schema(description = "필드", example = "email") String field,
		@Schema(description = "사유") String message) {
	}
}
