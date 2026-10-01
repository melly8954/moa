package com.moa.common.exception;

import java.util.List;

import com.moa.common.response.ErrorResponse.FieldError;

import lombok.Getter;

/**
 * 업무 흐름에서 의도적으로 던지는 예외. 응답은 GlobalExceptionHandler가 ErrorCode로 만든다.
 */
@Getter
public class ServiceException extends RuntimeException {

	private final ErrorCode errorCode;
	private final List<FieldError> errors;

	public ServiceException(ErrorCode errorCode) {
		this(errorCode, errorCode.getMessage());
	}

	/**
	 * @param message 사용자에게 보여줄 문장. 기본 문장 대신 쓴다
	 */
	public ServiceException(ErrorCode errorCode, String message) {
		this(errorCode, message, List.of());
	}

	/**
	 * @param errors 필드별 사유. V001(입력 검증)일 때만 쓴다
	 */
	public ServiceException(ErrorCode errorCode, String message, List<FieldError> errors) {
		super(message);
		this.errorCode = errorCode;
		this.errors = List.copyOf(errors);
	}
}
