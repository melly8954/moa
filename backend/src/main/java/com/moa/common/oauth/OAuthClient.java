package com.moa.common.oauth;

/**
 * 구글·카카오 OAuth 포트. 어댑터가 제공자별 주소, 클라이언트 키, 콜백 주소를 가진다.
 * Service는 이 인터페이스만 부른다.
 */
public interface OAuthClient {

	/**
	 * 제공자 동의 화면 주소.
	 *
	 * @param state CSRF 방지 값. 콜백에서 같은 값이 돌아와야 한다
	 */
	String authorizationUrl(OAuthProvider provider, String state);

	/**
	 * 인가 코드를 교환하고 사용자 정보를 읽는다.
	 *
	 * @throws OAuthException 코드 교환·토큰 검증·사용자 정보 조회 실패, 타임아웃
	 */
	OAuthUser fetchUser(OAuthProvider provider, String code);
}
