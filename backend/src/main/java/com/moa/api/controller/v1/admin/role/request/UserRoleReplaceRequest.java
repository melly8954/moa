package com.moa.api.controller.v1.admin.role.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "사용자 역할 바꾸기 요청")
public record UserRoleReplaceRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "줄 역할 ID 전부. 빈 목록이면 모두 뺀다",
		example = "[1, 2]") @NotNull List<@NotNull Long> roleIds) {
}
