package com.moa.common.security.token;

import java.time.Duration;

/**
 * @param accessToken 액세스 토큰(JWT)
 * @param accessTokenExpiresIn 액세스 토큰 남은 시간(초)
 * @param refreshToken 리프레시 토큰 원문. 응답으로 내보낸 뒤에는 서버에 남지 않는다
 * @param refreshTokenTtl 리프레시 토큰 수명. 쿠키 Max-Age에 쓴다
 */
public record IssuedTokens(String accessToken, long accessTokenExpiresIn, String refreshToken,
	Duration refreshTokenTtl) {
}
