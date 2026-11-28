package com.moa.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 에러 코드 목록.
 * 에러 코드 목록의 정본이다. 코드 형식은 영문 대문자 한 글자(V 검증, A 인증·인가, R 리소스, B 업무, S 시스템) + 세 자리 숫자.
 * 기본 코드는 psw-dev-kit CONTRACT.md 4절의 표와 같다. 프론트 템플릿이 이 코드로 화면 문구를 고른다.
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
	INVALID_CREDENTIALS("A006", HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
	ACCOUNT_LOCKED("A007", HttpStatus.UNAUTHORIZED, "잠긴 계정입니다. 관리자에게 문의해 주세요."),
	// moa 010 소셜 로그인 (specs/010-social-login contracts/auth-oauth.md). A010·A013·A016은 US5·FR-015 때 더한다
	OAUTH_PROVIDER_DISABLED("A008", HttpStatus.NOT_FOUND, "사용할 수 없는 로그인 방식입니다."),
	OAUTH_FAILED("A009", HttpStatus.BAD_REQUEST, "소셜 로그인에 실패했습니다. 다시 시도해 주세요."),
	SOCIAL_MEMBER_ROLE_NOT_ALLOWED("A011", HttpStatus.CONFLICT, "소셜 계정 회원에게는 관리 역할을 줄 수 없습니다."),
	SOCIAL_ACCOUNT_IN_USE("A012", HttpStatus.CONFLICT, "이미 다른 회원에 연결된 소셜 계정입니다."),
	SOCIAL_EMAIL_UNVERIFIED("A014", HttpStatus.BAD_REQUEST, "제공자에서 이메일 인증을 마친 뒤 다시 시도해 주세요."),
	SOCIAL_EMAIL_IN_USE("A015", HttpStatus.CONFLICT, "다른 방식으로 가입된 이메일입니다."),
	TERMS_NOT_AGREED("A017", HttpStatus.BAD_REQUEST, "필수 약관에 동의해 주세요."),
	// moa 009 마이페이지 (specs/009-mypage contracts/profile-api.md)
	MYPAGE_NOT_ALLOWED("A201", HttpStatus.FORBIDDEN, "관리자 계정은 마이페이지를 쓸 수 없습니다."),
	// moa 018 회원 탈퇴 (specs/018-member-withdrawal contracts/withdrawal-api.md). 본인 확인에 성공한 다음에만 쓴다
	ACCOUNT_WITHDRAWN("A401", HttpStatus.UNAUTHORIZED, "탈퇴한 계정입니다. 유예 기간 안에 복구할 수 있습니다."),
	ACCOUNT_PURGE_PENDING("A402", HttpStatus.UNAUTHORIZED, "복구할 수 있는 기간이 지난 계정입니다."),
	RECOVERY_NOT_AVAILABLE("A403", HttpStatus.BAD_REQUEST, "복구할 수 없습니다. 다시 로그인해 주세요."),

	// R: 리소스
	NOT_FOUND("R001", HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다."),
	DUPLICATE_RESOURCE("R002", HttpStatus.CONFLICT, "이미 존재합니다."),

	// B: 업무 규칙
	INVALID_STATE("B001", HttpStatus.CONFLICT, "현재 상태에서 할 수 없는 요청입니다."),
	// 닉네임 (specs/009-mypage research R4). 010 가입·009 변경·관리자 이름 바꾸기가 함께 쓴다
	NICKNAME_TAGS_EXHAUSTED("B201", HttpStatus.CONFLICT, "이 닉네임은 더 쓸 수 없습니다. 다른 닉네임을 골라 주세요."),
	// 다시 바꿀 수 있는 시각은 오류에 싣지 않고 프로필 조회의 nicknameChangeableAt으로 준다 (specs/009-mypage research R5)
	NICKNAME_CHANGE_TOO_SOON("B202", HttpStatus.CONFLICT, "아직 닉네임을 바꿀 수 없습니다."),
	// 고객센터 (specs/017-customer-center research R9)
	FAQ_LIMIT_EXCEEDED("B301", HttpStatus.CONFLICT, "FAQ는 200개까지 둘 수 있습니다."),

	// S: 시스템
	INTERNAL_ERROR("S001", HttpStatus.INTERNAL_SERVER_ERROR, "처리 중 문제가 발생했습니다.");

	private final String code;
	private final HttpStatus httpStatus;
	private final String message;
}
