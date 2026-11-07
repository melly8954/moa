package com.moa.api.controller.v1.admin.role.request;

import java.util.List;

import com.moa.api.application.v1.role.command.CreateAccessRoleCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "역할 만들기 요청")
public record RoleCreateRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "역할 코드. 영문 대문자·숫자·밑줄, 만든 뒤 바꾸지 않는다",
		example = "MANAGER") @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{0,49}") String code,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "이름",
		example = "운영 매니저") @NotBlank @Size(max = 50) String name,
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "설명") @Size(max = 200) String description,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "묶을 권한 키. 등록되지 않은 키는 V001",
		example = "[\"CODE_MANAGE\"]") @NotNull List<@NotBlank String> permissions) {

	public CreateAccessRoleCommand toCommand() {
		return new CreateAccessRoleCommand(code, name, description, permissions);
	}
}
