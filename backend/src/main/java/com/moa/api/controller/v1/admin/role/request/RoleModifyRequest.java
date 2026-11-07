package com.moa.api.controller.v1.admin.role.request;

import java.util.List;

import com.moa.api.application.v1.role.command.ModifyAccessRoleCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "역할 바꾸기 요청. 코드는 바꾸지 않는다")
public record RoleModifyRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "이름",
		example = "운영 매니저") @NotBlank @Size(max = 50) String name,
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "설명") @Size(max = 200) String description,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "묶을 권한 키 (통째로 바꾼다)",
		example = "[\"CODE_MANAGE\"]") @NotNull List<@NotBlank String> permissions) {

	public ModifyAccessRoleCommand toCommand(Long id) {
		return new ModifyAccessRoleCommand(id, name, description, permissions);
	}
}
