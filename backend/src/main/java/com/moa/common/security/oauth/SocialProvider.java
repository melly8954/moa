package com.moa.common.security.oauth;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * 소셜 로그인 제공자 (specs/010-social-login). 경로 값은 소문자 이름이다 ({@code /api/v1/auth/oauth/google/...}).
 * 디스코드는 다음 단계에서 더한다.
 */
public enum SocialProvider {
	GOOGLE;

	/** 경로·쿼리에 쓰는 값 (google) */
	public String pathValue() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static Optional<SocialProvider> fromPathValue(String value) {
		if (value == null) {
			return Optional.empty();
		}
		return Arrays.stream(values()).filter(provider -> provider.pathValue().equals(value)).findFirst();
	}
}
