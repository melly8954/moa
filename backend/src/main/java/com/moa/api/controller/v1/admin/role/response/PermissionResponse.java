package com.moa.api.controller.v1.admin.role.response;

import com.moa.common.security.permission.PermissionDefinition;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "권한 (코드에 등록된 것)")
public record PermissionResponse(
	@Schema(description = "권한 키", example = "CODE_MANAGE") String key,
	@Schema(description = "이름", example = "공통코드 관리") String name,
	@Schema(description = "화면에서 묶을 이름", example = "공통코드") String group) {

	public static PermissionResponse from(PermissionDefinition definition) {
		return new PermissionResponse(definition.key(), definition.name(), definition.group());
	}
}
