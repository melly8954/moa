package com.moa.api.controller.v1.response;

import java.time.Instant;

import com.moa.api.dto.PhoneVerificationDto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "보낸 문자 인증 번호")
public record PhoneVerificationGetResponse(
	@Schema(description = "인증 번호 만료 시각") Instant expiresAt,
	@Schema(description = "다시 받을 수 있는 시각") Instant resendAvailableAt) {

	public static PhoneVerificationGetResponse from(PhoneVerificationDto dto) {
		return new PhoneVerificationGetResponse(dto.expiresAt(), dto.resendAvailableAt());
	}
}
