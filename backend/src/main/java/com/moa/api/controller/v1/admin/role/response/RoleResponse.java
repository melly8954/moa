package com.moa.api.controller.v1.admin.role.response;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.role.AccessRoleDto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "역할")
public record RoleResponse(
	@Schema(description = "역할 ID") Long id,
	@Schema(description = "역할 코드", example = "MANAGER") String code,
	@Schema(description = "이름", example = "운영 매니저") String name,
	@Schema(description = "설명") @Nullable String description,
	@Schema(description = "묶은 권한 키 (이름순)", example = "[\"CODE_MANAGE\"]") List<String> permissions,
	@Schema(description = "이 역할을 가진 사용자 수", example = "3") long userCount) {

	public static RoleResponse from(AccessRoleDto role) {
		return new RoleResponse(role.id(), role.code(), role.name(), role.description(), role.permissions(),
			role.userCount());
	}
}
