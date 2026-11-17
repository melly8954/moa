package com.moa.api.controller.v1.admin.code.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "moa: 한 그룹 안 여러 항목의 사용 여부 한꺼번에 바꾸기 요청")
public record CodeEnabledRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "코드들",
		example = "[\"001\", \"002\"]") @NotEmpty @Size(max = 200) List<@NotBlank String> codes,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "사용 여부") @NotNull Boolean enabled) {
}
