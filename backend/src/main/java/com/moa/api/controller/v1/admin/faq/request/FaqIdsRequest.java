package com.moa.api.controller.v1.admin.faq.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "FAQ ID 목록 (한꺼번에 지우기)")
public record FaqIdsRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "FAQ ID들",
		example = "[1, 2]") @NotEmpty @Size(max = 200) List<@NotNull Long> ids) {
}
