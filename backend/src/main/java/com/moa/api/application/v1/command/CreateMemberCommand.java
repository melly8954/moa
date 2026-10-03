package com.moa.api.application.v1.command;

/**
 * 가입 완료.
 *
 * @param signUpToken 가입 진행 토큰
 * @param nickname 닉네임
 * @param termsAgreed 이용약관 동의
 * @param privacyAgreed 개인정보 수집·이용 동의
 */
public record CreateMemberCommand(String signUpToken, String nickname, boolean termsAgreed,
	boolean privacyAgreed) {
}
