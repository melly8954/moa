package com.moa.api.controller.v1.admin.menu.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "moa: 여러 메뉴의 사용 여부 한꺼번에 바꾸기 요청")
public record MenuEnabledRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "메뉴 ID들",
		example = "[3, 4]") @NotEmpty @Size(max = 200) List<@NotNull Long> ids,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "사용 여부") @NotNull Boolean enabled) {
}
