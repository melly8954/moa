package com.moa.common.response;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.moa.common.exception.ErrorCode;
import com.moa.common.logging.TraceIdFilter;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 오류 응답. 형식은 docs/design/conventions.md REST 절을 따른다.
 */
@Schema(description = "오류 응답")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
	@Schema(description = "에러 코드", example = "V001") String code,
	@Schema(description = "사용자에게 보여줄 문장") String message,
	@Schema(description = "발생 시각") OffsetDateTime timestamp,
	@Schema(description = "요청 경로") String path,
	@Schema(description = "요청 추적 ID. 서버 로그와 같은 값") String traceId,
	@Schema(description = "필드별 검증 오류. V001일 때만 있다") List<FieldError> errors) {

	public static ErrorResponse of(ErrorCode errorCode, String message, String path, List<FieldError> errors) {
		return new ErrorResponse(errorCode.getCode(), message, OffsetDateTime.now(), path,
			TraceIdFilter.currentTraceId(),
			errors);
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
