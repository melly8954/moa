package com.moa.api.controller.v1.admin.role.response;

import java.util.List;

import com.moa.api.dto.role.RoleUserDto;
import com.moa.common.security.Role;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "moa: 역할 관리에서 보는 사용자 (specs/003-admin-roles FR-005·FR-006)")
public record RoleUserResponse(
	@Schema(description = "사용자 ID") Long id,
	@Schema(description = "아이디", example = "operator") String loginId,
	@Schema(description = "이름", example = "김운영") String name,
	@Schema(description = "시스템 역할. ADMIN은 모든 권한을 가져 업무 역할을 받지 않는다", example = "USER") Role role,
	@Schema(description = "받은 업무 역할 (만든 순서)") List<AccessRoleRef> accessRoles) {

	public static RoleUserResponse from(RoleUserDto user) {
		return new RoleUserResponse(user.id(), user.loginId(), user.name(), user.role(),
			user.accessRoles().stream().map(role -> new AccessRoleRef(role.id(), role.code(), role.name())).toList());
	}

	@Schema(description = "업무 역할")
	public record AccessRoleRef(
		@Schema(description = "역할 ID") Long id,
		@Schema(description = "역할 코드", example = "SECURITY") String code,
		@Schema(description = "이름", example = "보안 담당") String name) {
	}
}
