package com.moa.api.dto;

/**
 * 이메일 인증 링크를 연 결과.
 */
public enum EmailConfirmationResult {
	/** 새 가입을 이어 간다. 다음은 생년월일·휴대폰 인증이다 */
	CONTINUE_SIGN_UP,
	/** 같은 이메일의 기존 계정에 연결(비밀번호 추가, 카카오 연결)하고 로그인했다 */
	SIGNED_IN
}
