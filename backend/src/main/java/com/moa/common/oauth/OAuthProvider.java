package com.moa.common.oauth;

import java.util.Arrays;
import java.util.Optional;

/**
 * 소셜 로그인 제공자. 경로 값은 소문자(google, kakao)다.
 */
public enum OAuthProvider {
	GOOGLE,
	KAKAO;

	/**
	 * @param pathValue 경로의 제공자 값 (google, kakao)
	 */
	public static Optional<OAuthProvider> fromPathValue(String pathValue) {
		return Arrays.stream(values())
			.filter(provider -> provider.pathValue().equals(pathValue))
			.findFirst();
	}

	public String pathValue() {
		return name().toLowerCase();
	}
}
