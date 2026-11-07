package com.moa.api.controller.v1.admin.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 정보 바꾸기 요청")
public record UserRenameRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "이름",
		example = "홍길동") @NotBlank @Size(max = 50) String name) {
}
