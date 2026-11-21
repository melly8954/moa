package com.moa.common.security.oauth;

import java.util.Optional;

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
	 * 인가 코드를 바꾸고 신원을 확인한다. 콜백은 실패를 오류 응답이 아니라 화면 이동(oauthError=failed)으로 알리므로
	 * 예외를 던지지 않고 빈 값을 돌려준다. 실패 이유는 구현이 로그에 남긴다.
	 *
	 * @return 확인한 신원. 교환·확인에 실패하면 빈 값
	 */
	Optional<OAuthIdentity> exchange(String code, String codeVerifier, String nonce);
}
