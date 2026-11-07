package com.moa.api.controller.v1.admin.user.request;

import com.moa.common.security.Role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "시스템 역할 바꾸기 요청")
public record UserRoleChangeRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "시스템 역할",
		example = "ADMIN") @NotNull Role role) {
}
