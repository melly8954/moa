package com.moa.api.dto.user;

/**
 * 연결된 회원이 없는 소셜 계정의 가입 확인 결과 (specs/010-social-login research R12).
 * 콜백은 오류 응답이 아니라 화면 이동으로 알리므로 예외 대신 결과로 돌려준다.
 *
 * @param email 정규화한(공백 제거·소문자) 이메일. 이메일이 없으면 null
 */
public record SocialSignUpCheck(Result result, String email) {

	public enum Result {
		ALLOWED,
		/** 이메일이 없거나 제공자가 인증하지 않았다 (A014) */
		EMAIL_UNVERIFIED,
		/** 다른 회원이 쓰는 이메일 (A015) */
		EMAIL_IN_USE
	}

	public boolean allowed() {
		return result == Result.ALLOWED;
	}
}
