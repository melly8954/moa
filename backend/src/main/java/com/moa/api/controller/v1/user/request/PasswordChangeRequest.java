package com.moa.api.controller.v1.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "내 비밀번호 바꾸기 요청")
public record PasswordChangeRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
		description = "지금 비밀번호 (임시 비밀번호 포함)") @NotBlank @Size(max = 100) String currentPassword,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "새 비밀번호. 8~100자") @NotBlank @Size(min = 8,
		max = 100) String newPassword) {
}
