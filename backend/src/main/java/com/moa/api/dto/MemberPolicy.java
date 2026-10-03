package com.moa.api.dto;

import java.time.Duration;

/**
 * 회원 정책 수치. 값의 정본은 docs/req/functional/member/_policy.md와 SEC-AUTH·INT 요구사항이다.
 * 화면 검증(zod)도 같은 값을 쓴다.
 */
public final class MemberPolicy {

	/** 닉네임: 2~12자, 한글·영문·숫자·밑줄. 중복 검사는 DB 정렬(utf8mb4_unicode_ci)로 대소문자를 무시한다 */
	public static final String NICKNAME_PATTERN = "^[가-힣A-Za-z0-9_]{2,12}$";

	/** 비밀번호: 8~20자, 영문과 숫자를 각각 1자 이상 */
	public static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*[0-9]).{8,20}$";

	/** 가입 나이: 만 14세 이상 */
	public static final int MIN_SIGN_UP_AGE = 14;

	/** 휴대폰 번호: 숫자만 (예: 01012345678) */
	public static final String PHONE_NUMBER_PATTERN = "^01[016789][0-9]{7,8}$";

	/** 문자 인증 번호: 6자리 숫자 */
	public static final String VERIFICATION_CODE_PATTERN = "^[0-9]{6}$";

	public static final int EMAIL_MAX_LENGTH = 255;

	/** 이메일 인증 링크 유효 시간 */
	public static final Duration EMAIL_LINK_TTL = Duration.ofMinutes(30);

	/** 문자 인증 번호 유효 시간 */
	public static final Duration PHONE_CODE_TTL = Duration.ofMinutes(3);

	/** 문자 인증 번호 입력 가능 횟수 */
	public static final int PHONE_CODE_MAX_ATTEMPTS = 5;

	/** 메일·문자 다시 보내기 간격 */
	public static final Duration RESEND_INTERVAL = Duration.ofSeconds(60);

	/** 한 휴대폰 번호에 하루 보낼 수 있는 문자 수 */
	public static final int DAILY_SMS_LIMIT = 10;

	private MemberPolicy() {
	}
}
