package com.moa.api.controller.v1.admin.code.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "코드 그룹 만들기 요청")
public record CodeGroupCreateRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "그룹 코드. 영문 대문자·숫자·밑줄",
		example = "ORDER_STATUS") @NotBlank @Size(max = 50) @Pattern(regexp = "[A-Z0-9_]+") String groupCode,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "이름",
		example = "주문 상태") @NotBlank @Size(max = 100) String name,
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "설명") @Size(max = 500) String description) {
}
