package com.moa.common.exception;

import java.util.List;
import java.util.Map;

import com.moa.common.response.ErrorResponse.FieldError;

import lombok.Getter;

/**
 * 업무 흐름에서 의도적으로 던지는 예외. 응답은 GlobalExceptionHandler가 ErrorCode로 만든다.
 */
@Getter
public class ServiceException extends RuntimeException {

	private final ErrorCode errorCode;
	private final List<FieldError> errors;
	private final Map<String, Object> details;

	public ServiceException(ErrorCode errorCode) {
		this(errorCode, errorCode.getMessage());
	}

	/**
	 * @param message 기본 문장 대신 쓸 설명 문장
	 */
	public ServiceException(ErrorCode errorCode, String message) {
		this(errorCode, message, List.of());
	}

	/**
	 * @param errors 필드별 사유. V001(입력 검증)일 때만 쓴다
	 */
	public ServiceException(ErrorCode errorCode, String message, List<FieldError> errors) {
		this(errorCode, message, errors, Map.of());
	}

	/**
	 * @param details 화면 안내에 끼울 값 (예: lockedUntil). 문장이 아닌 데이터, 시각은 ISO-8601. null 값은 넣지 않는다
	 */
	public ServiceException(ErrorCode errorCode, String message, List<FieldError> errors,
		Map<String, Object> details) {
		super(message);
		this.errorCode = errorCode;
		this.errors = List.copyOf(errors);
		this.details = Map.copyOf(details);
	}

	/**
	 * 화면 안내에 값이 필요한 업무 오류. 예: {@code ServiceException.withDetails(ErrorCode.X, Map.of("lockedUntil", until))}
	 */
	public static ServiceException withDetails(ErrorCode errorCode, Map<String, Object> details) {
		return new ServiceException(errorCode, errorCode.getMessage(), List.of(), details);
	}
}
