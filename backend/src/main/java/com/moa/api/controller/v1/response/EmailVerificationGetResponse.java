package com.moa.api.controller.v1.response;

import java.time.Instant;

import com.moa.api.dto.EmailVerificationDto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "보낸 인증 메일")
public record EmailVerificationGetResponse(
	@Schema(description = "보낸 주소") String email,
	@Schema(description = "링크 만료 시각") Instant expiresAt,
	@Schema(description = "다시 보낼 수 있는 시각") Instant resendAvailableAt) {

	public static EmailVerificationGetResponse from(EmailVerificationDto dto) {
		return new EmailVerificationGetResponse(dto.email(), dto.expiresAt(), dto.resendAvailableAt());
	}
}
