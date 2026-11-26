package com.moa.api.controller.v1.auth.response;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Objects;

import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "탈퇴한 계정의 복구 안내")
public record RecoveryResponse(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
		description = "완전 삭제 예정 시각. 이때까지 복구할 수 있다") OffsetDateTime purgeAt) {

	public static RecoveryResponse from(LocalDateTime purgeAt) {
		return new RecoveryResponse(Objects.requireNonNull(ApiTimes.of(purgeAt)));
	}
}
