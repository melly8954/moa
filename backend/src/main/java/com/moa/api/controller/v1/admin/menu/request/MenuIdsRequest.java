package com.moa.api.controller.v1.admin.menu.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "moa: 메뉴 ID 목록 (한꺼번에 지우기)")
public record MenuIdsRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "메뉴 ID들",
		example = "[3, 4]") @NotEmpty @Size(max = 200) List<@NotNull Long> ids) {
}
