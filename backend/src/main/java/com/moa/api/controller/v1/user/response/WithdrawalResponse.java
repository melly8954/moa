package com.moa.api.controller.v1.user.response;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Objects;

import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원 탈퇴 결과")
public record WithdrawalResponse(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
		description = "완전 삭제 예정 시각. 이때까지 다시 로그인해 복구할 수 있다") OffsetDateTime purgeAt) {

	public static WithdrawalResponse from(LocalDateTime purgeAt) {
		return new WithdrawalResponse(Objects.requireNonNull(ApiTimes.of(purgeAt)));
	}
}
