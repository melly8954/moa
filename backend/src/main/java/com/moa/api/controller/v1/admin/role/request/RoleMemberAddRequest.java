package com.moa.api.controller.v1.admin.role.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "moa: 역할에 구성원 더하기 요청")
public record RoleMemberAddRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "더할 사용자 ID",
		example = "10") @NotNull Long userId) {
}
