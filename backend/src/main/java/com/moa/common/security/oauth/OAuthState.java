package com.moa.common.security.oauth;

/**
 * 인가 시작 때 심고 콜백에서 확인하는 값 (oauth_state 쿠키, research R3).
 *
 * @param nonce OIDC nonce. 쓰지 않는 제공자는 null
 * @param next 로그인 뒤 돌아갈 주소. {@link OAuthNextPath}로 거른 값이거나 null
 */
public record OAuthState(SocialProvider provider, String state, String nonce, String codeVerifier, OAuthMode mode,
	String next) {
}
