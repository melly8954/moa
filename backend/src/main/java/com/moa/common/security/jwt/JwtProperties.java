package com.moa.common.security.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @param secret HS256 서명 키. 32바이트 이상이어야 한다
 * @param issuer 토큰 발급자(iss)
 * @param accessTokenTtl 액세스 토큰 수명
 * @param refreshTokenTtl 리프레시 토큰 수명
 */
@Validated
@ConfigurationProperties("psw.jwt")
public record JwtProperties(
	@NotBlank String secret,
	@NotBlank String issuer,
	@NotNull Duration accessTokenTtl,
	@NotNull Duration refreshTokenTtl) {

	private static final int MIN_SECRET_BYTES = 32;

	@AssertTrue(message = "psw.jwt.secret(JWT_SECRET)은 32바이트 이상이어야 합니다.")
	public boolean isSecretLongEnough() {
		return secret == null || secret.getBytes(StandardCharsets.UTF_8).length >= MIN_SECRET_BYTES;
	}
}
