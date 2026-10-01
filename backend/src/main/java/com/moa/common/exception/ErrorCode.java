package com.moa.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 에러 코드 목록.
 * 에러 코드 목록의 정본이다. 형식과 번호 규칙은 docs/design/conventions.md REST 절을 따른다.
 * 기본 코드는 하네스 conventions.md 템플릿의 기본 표와 같다 (harness-psw 4.7).
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// V: 입력 검증
	INVALID_INPUT("V001", HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
	METHOD_NOT_ALLOWED("V002", HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다."),
	UNSUPPORTED_MEDIA_TYPE("V003", HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 요청 형식입니다."),

	// A: 인증·인가
	UNAUTHORIZED("A001", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
	ACCESS_TOKEN_EXPIRED("A002", HttpStatus.UNAUTHORIZED, "액세스 토큰이 만료되었습니다."),
	INVALID_REFRESH_TOKEN("A003", HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다."),
	REFRESH_TOKEN_REUSED("A004", HttpStatus.UNAUTHORIZED, "이미 사용된 리프레시 토큰입니다. 다시 로그인해 주세요."),
	FORBIDDEN("A005", HttpStatus.FORBIDDEN, "권한이 없습니다."),

	// R: 리소스
	NOT_FOUND("R001", HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다."),
	DUPLICATE_RESOURCE("R002", HttpStatus.CONFLICT, "이미 존재합니다."),

	// B: 업무 규칙
	INVALID_STATE("B001", HttpStatus.CONFLICT, "현재 상태에서 할 수 없는 요청입니다."),

	// S: 시스템
	INTERNAL_ERROR("S001", HttpStatus.INTERNAL_SERVER_ERROR, "처리 중 문제가 발생했습니다.");

	private final String code;
	private final HttpStatus httpStatus;
	private final String message;
}
