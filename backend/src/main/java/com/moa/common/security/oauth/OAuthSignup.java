package com.moa.common.security.oauth;

/**
 * 가입 확인을 통과하고 닉네임·약관을 기다리는 신원 (oauth_signup 쿠키, research R5). 회원은 아직 없다.
 *
 * @param email 정규화한(공백 제거·소문자) 인증 이메일
 */
public record OAuthSignup(SocialProvider provider, String subject, String email, String name, String next) {
}
