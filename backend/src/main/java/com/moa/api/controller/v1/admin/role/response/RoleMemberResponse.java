package com.moa.api.controller.v1.admin.role.response;

import com.moa.api.dto.role.RoleMemberDto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "moa: 역할 구성원의 한 줄 (specs/003-admin-roles FR-006). 관리자 계정만 보인다")
public record RoleMemberResponse(
	@Schema(description = "사용자 ID") Long id,
	@Schema(description = "아이디", example = "operator") String loginId,
	@Schema(description = "이름", example = "김운영") String name,
	@Schema(description = "최고 관리자 역할을 가졌나. 다른 역할은 받지 않는다") boolean superAdmin,
	@Schema(description = "이 역할의 구성원인가") boolean member) {

	public static RoleMemberResponse from(RoleMemberDto dto) {
		return new RoleMemberResponse(dto.id(), dto.loginId(), dto.name(), dto.superAdmin(), dto.member());
	}
}
