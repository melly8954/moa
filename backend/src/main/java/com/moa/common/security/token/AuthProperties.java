package com.moa.common.security.token;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @param refreshCookie 웹에서 리프레시 토큰을 담는 쿠키
 * @param appClientEnabled 앱(X-Client-Type: app)에 리프레시 토큰을 본문으로 주는지
 */
@Validated
@ConfigurationProperties("psw.auth")
public record AuthProperties(@Valid @NotNull RefreshCookie refreshCookie, boolean appClientEnabled) {

	public record RefreshCookie(@NotBlank String name, @NotBlank String path, boolean secure,
		@NotBlank String sameSite) {
	}
}
