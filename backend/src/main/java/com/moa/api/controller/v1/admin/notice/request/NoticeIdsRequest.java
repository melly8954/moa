package com.moa.api.controller.v1.admin.notice.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "공지 ID 목록 (한꺼번에 지우기)")
public record NoticeIdsRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "공지 ID들",
		example = "[1, 2]") @NotEmpty @Size(max = 100) List<@NotNull Long> ids) {
}
