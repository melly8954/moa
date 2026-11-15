package com.moa.api.controller.v1.admin.role.response;

import com.moa.api.dto.role.RoleMemberDto;
import com.moa.common.security.Role;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "moa: 역할 드로어 구성원 탭의 사용자 (specs/003-admin-roles FR-006)")
public record RoleMemberResponse(
	@Schema(description = "사용자 ID") Long id,
	@Schema(description = "아이디", example = "operator") String loginId,
	@Schema(description = "이름", example = "김운영") String name,
	@Schema(description = "시스템 역할. ADMIN은 업무 역할을 받지 않는다", example = "USER") Role role,
	@Schema(description = "이 역할의 구성원인가") boolean member) {

	public static RoleMemberResponse from(RoleMemberDto dto) {
		return new RoleMemberResponse(dto.id(), dto.loginId(), dto.name(), dto.role(), dto.member());
	}
}
