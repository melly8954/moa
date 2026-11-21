package com.moa.common.security.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 소셜 로그인 제공자 설정 (specs/010-social-login research R10). 값이 비면 그 제공자를 끈다 (단추 숨김, 시작·콜백 A008).
 *
 * @param google 구글 OAuth 클라이언트
 */
@ConfigurationProperties("app.oauth")
public record OAuthProperties(Client google) {

	public OAuthProperties {
		google = google == null ? new Client(null, null, null) : google;
	}

	/**
	 * @param redirectUri 프론트 주소 + /api/v1/auth/oauth/{provider}/callback (제공자 콘솔에 같은 값을 등록)
	 */
	public record Client(String clientId, String clientSecret, String redirectUri) {

		/** 셋 다 있어야 켜진다 */
		public boolean enabled() {
			return hasText(clientId) && hasText(clientSecret) && hasText(redirectUri);
		}

		private static boolean hasText(String value) {
			return value != null && !value.isBlank();
		}
	}
}
