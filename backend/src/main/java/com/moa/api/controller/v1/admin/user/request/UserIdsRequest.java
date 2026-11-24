package com.moa.api.controller.v1.admin.user.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "moa: 사용자 ID 목록 (한꺼번에 지우기)")
public record UserIdsRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "사용자 ID들",
		example = "[1, 2]") @NotEmpty @Size(max = 100) List<@NotNull Long> ids) {
}
