package com.moa.api.controller.v1.admin.code.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

@Schema(description = "moa: 한 그룹 안 여러 항목 고르기 (일괄 삭제)")
public record CodesRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "코드들",
		example = "[\"001\", \"002\"]") @NotEmpty @Size(max = 200) List<@NotBlank String> codes) {
}
