package com.moa.api.controller.v1.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.moa.api.dto.EmailConfirmationDto;
import com.moa.api.dto.EmailConfirmationResult;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이메일 인증 결과. SIGNED_IN이면 토큰을 함께 준다 (리프레시 토큰은 쿠키)")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmailConfirmationGetResponse(
	@Schema(description = "CONTINUE_SIGN_UP: 휴대폰 인증으로 이어 감. SIGNED_IN: 기존 계정에 연결·로그인") EmailConfirmationResult result,
	@Schema(description = "액세스 토큰. SIGNED_IN일 때만") String accessToken,
	@Schema(description = "토큰 유형. SIGNED_IN일 때만", example = "Bearer") String tokenType,
	@Schema(description = "액세스 토큰 남은 시간(초). SIGNED_IN일 때만", example = "900") Long expiresIn,
	@Schema(description = "리프레시 토큰. SIGNED_IN이고 앱(X-Client-Type: app)일 때만") String refreshToken) {

	/**
	 * @param bodyRefreshToken RefreshTokenTransport.write가 돌려준 값. 웹이거나 로그인하지 않았으면 null
	 */
	public static EmailConfirmationGetResponse from(EmailConfirmationDto dto, String bodyRefreshToken) {
		if (dto.tokens() == null) {
			return new EmailConfirmationGetResponse(dto.result(), null, null, null, null);
		}
		return new EmailConfirmationGetResponse(dto.result(), dto.tokens().accessToken(), "Bearer",
			dto.tokens().accessTokenExpiresIn(), bodyRefreshToken);
	}
}
