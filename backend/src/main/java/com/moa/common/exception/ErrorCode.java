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
	SIGN_UP_REQUIRED("A006", HttpStatus.UNAUTHORIZED, "가입 진행 정보가 없거나 만료되었습니다. 처음부터 다시 가입해 주세요."),
	OAUTH_FAILED("A007", HttpStatus.UNAUTHORIZED, "구글·카카오 인증을 마치지 못했습니다."),

	// R: 리소스
	NOT_FOUND("R001", HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다."),
	DUPLICATE_RESOURCE("R002", HttpStatus.CONFLICT, "이미 존재합니다."),
	NICKNAME_TAKEN("R003", HttpStatus.CONFLICT, "이미 쓰이는 닉네임입니다."),

	// B: 업무 규칙
	INVALID_STATE("B001", HttpStatus.CONFLICT, "현재 상태에서 할 수 없는 요청입니다."),
	EMAIL_ALREADY_REGISTERED("B002", HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
	SOCIAL_EMAIL_CONFLICT("B003", HttpStatus.CONFLICT, "같은 이메일로 가입된 계정이 있어 합칠 수 없습니다."),
	VERIFICATION_LINK_INVALID("B004", HttpStatus.BAD_REQUEST, "인증 링크가 만료되었거나 이미 쓰였습니다."),
	VERIFICATION_CODE_MISMATCH("B005", HttpStatus.BAD_REQUEST, "인증 번호가 맞지 않습니다."),
	VERIFICATION_CODE_EXPIRED("B006", HttpStatus.BAD_REQUEST, "인증 번호가 만료되었습니다."),
	VERIFICATION_ATTEMPTS_EXCEEDED("B007", HttpStatus.BAD_REQUEST, "인증 번호 입력 횟수를 넘었습니다."),
	RESEND_TOO_SOON("B008", HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 보내 주세요."),
	DAILY_SEND_LIMIT_EXCEEDED("B009", HttpStatus.TOO_MANY_REQUESTS, "오늘 보낼 수 있는 횟수를 넘었습니다."),
	UNDER_SIGN_UP_AGE("B010", HttpStatus.BAD_REQUEST, "가입할 수 있는 나이가 아닙니다."),
	PHONE_ALREADY_REGISTERED("B011", HttpStatus.CONFLICT, "이미 가입된 휴대폰 번호입니다."),
	PHONE_OWNER_SUSPENDED("B012", HttpStatus.CONFLICT, "이용이 정지된 계정의 휴대폰 번호입니다."),
	PHONE_OWNER_WITHDRAWN("B013", HttpStatus.CONFLICT, "최근 탈퇴한 계정의 휴대폰 번호입니다."),
	PHONE_REJOIN_RESTRICTED("B014", HttpStatus.CONFLICT, "지금은 이 휴대폰 번호로 가입할 수 없습니다."),
	SIGN_UP_INCOMPLETE("B015", HttpStatus.CONFLICT, "가입에 필요한 인증을 마치지 않았습니다."),

	// S: 시스템
	INTERNAL_ERROR("S001", HttpStatus.INTERNAL_SERVER_ERROR, "처리 중 문제가 발생했습니다."),
	MAIL_SEND_FAILED("S002", HttpStatus.SERVICE_UNAVAILABLE, "메일을 보내지 못했습니다."),
	SMS_SEND_FAILED("S003", HttpStatus.SERVICE_UNAVAILABLE, "문자를 보내지 못했습니다.");

	private final String code;
	private final HttpStatus httpStatus;
	private final String message;
}
