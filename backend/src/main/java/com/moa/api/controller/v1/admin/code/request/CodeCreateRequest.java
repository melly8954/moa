package com.moa.api.controller.v1.admin.code.request;

import org.jspecify.annotations.Nullable;

import com.moa.api.application.v1.code.command.CreateCodeCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "코드 만들기 요청")
public record CodeCreateRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
		description = "코드. 영문 대문자·숫자·밑줄. moa: 필수(자동 순번 없음, specs/004-admin-codes FR-006)",
		example = "PAID") @NotBlank @Size(max = 50) @Pattern(regexp = "[A-Z0-9_]+") String code,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "이름",
		example = "결제 완료") @NotBlank @Size(max = 100) String name,
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "정렬 순서. 작을수록 앞. moa: 비우면 그룹 맨 뒤",
		example = "10") @Min(0) @Nullable Integer sortOrder) {

	public CreateCodeCommand toCommand(String groupCode) {
		return new CreateCodeCommand(groupCode, code, name, sortOrder);
	}
}
