package com.moa.common.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 구글·카카오 OAuth 설정. 키는 환경변수로만 받는다. 운영 프로필은 기본값 없이 시작할 때 값이 있는지 검사한다.
 *
 * @param callbackBaseUrl 제공자가 돌아올 API 기본 주소 (OAUTH_CALLBACK_BASE_URL, 예: https://api.example.com)
 * @param google 구글 클라이언트 (GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET)
 * @param kakao 카카오 클라이언트 (KAKAO_CLIENT_ID, KAKAO_CLIENT_SECRET)
 */
@Validated
@ConfigurationProperties("psw.oauth")
public record OAuthProperties(@NotBlank String callbackBaseUrl, @Valid @NotNull Client google,
	@Valid @NotNull Client kakao) {

	/** 제공자가 인가 코드를 들고 돌아올 주소. 제공자 콘솔에 같은 값을 등록한다 */
	public String redirectUri(OAuthProvider provider) {
		String base = callbackBaseUrl.endsWith("/")
			? callbackBaseUrl.substring(0, callbackBaseUrl.length() - 1)
			: callbackBaseUrl;
		return base + "/api/v1/auth/oauth/" + provider.pathValue() + "/callback";
	}

	public Client client(OAuthProvider provider) {
		return provider == OAuthProvider.GOOGLE ? google : kakao;
	}

	/**
	 * @param clientId 클라이언트 ID (카카오는 REST API 키)
	 * @param clientSecret 클라이언트 시크릿
	 */
	public record Client(String clientId, String clientSecret) {
	}
}
