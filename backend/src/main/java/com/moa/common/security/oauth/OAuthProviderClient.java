package com.moa.common.security.oauth;

/**
 * 제공자와 주고받는 경계 (specs/010-social-login research R2). 키트 oauth 헬퍼가 생기면 이 구현을 바꾼다.
 * 제공자 토큰과 client secret은 이 경계 밖으로 나오지 않는다.
 */
public interface OAuthProviderClient {

	SocialProvider provider();

	/** 설정이 다 있어 쓸 수 있는지 */
	boolean enabled();

	/**
	 * 제공자 인가 화면 주소.
	 *
	 * @param nonce OIDC nonce. 쓰지 않는 제공자는 무시한다
	 * @param codeChallenge PKCE S256 challenge
	 */
	String authorizationUrl(String state, String nonce, String codeChallenge);

	/**
	 * 인가 코드를 바꾸고 신원을 확인한다.
	 *
	 * @throws com.moa.common.exception.ServiceException 교환·확인에 실패하면 A009
	 */
	OAuthIdentity exchange(String code, String codeVerifier, String nonce);
}
