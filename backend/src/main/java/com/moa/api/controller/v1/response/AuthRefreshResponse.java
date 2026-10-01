package com.moa.api.controller.v1.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.moa.common.security.token.IssuedTokens;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 발급 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthRefreshResponse(
	@Schema(description = "액세스 토큰") String accessToken,
	@Schema(description = "토큰 유형", example = "Bearer") String tokenType,
	@Schema(description = "액세스 토큰 남은 시간(초)", example = "900") long expiresIn,
	@Schema(description = "리프레시 토큰. 앱(X-Client-Type: app)에만 준다") String refreshToken) {

	/**
	 * @param bodyRefreshToken RefreshTokenTransport.write가 돌려준 값. 웹이면 null
	 */
	public static AuthRefreshResponse from(IssuedTokens tokens, String bodyRefreshToken) {
		return new AuthRefreshResponse(tokens.accessToken(), "Bearer", tokens.accessTokenExpiresIn(),
			bodyRefreshToken);
	}
}
