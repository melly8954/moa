package com.moa.api.controller.v1.admin.code.request;

import org.jspecify.annotations.Nullable;

import com.moa.api.application.v1.code.command.CreateCodeGroupCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "코드 그룹 만들기 요청. moa: 일반 그룹으로 만든다. 시스템 그룹은 마이그레이션으로만 생긴다")
public record CodeGroupCreateRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "그룹 코드. 영문 대문자로 시작, 대문자·숫자·밑줄. 만든 뒤 바꾸지 않는다",
		example = "EVENT_TYPE") @NotBlank @Size(max = 50) @Pattern(regexp = "[A-Z][A-Z0-9_]*") String groupCode,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "이름",
		example = "이벤트 분류") @NotBlank @Size(max = 100) String name,
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED,
		description = "설명") @Size(max = 500) @Nullable String description,
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "moa: 쓰는 곳",
		example = "이벤트 등록 화면의 분류") @Size(max = 200) @Nullable String usedBy) {

	public CreateCodeGroupCommand toCommand() {
		return new CreateCodeGroupCommand(groupCode, name, blankToNull(description), blankToNull(usedBy));
	}

	static @Nullable String blankToNull(@Nullable String value) {
		return value == null || value.isBlank() ? null : value;
	}
}
