package com.moa.api.dto.user;

import java.util.List;

import com.moa.api.dto.role.GrantBlock;
import com.moa.api.dto.role.RoleUserDto;

/**
 * 사용자 관리 목록·하나 조회의 한 줄 (moa, specs/002-admin-users FR-006). 업무 역할은 보기만 한다.
 *
 * @param accessRoles 받은 역할 (만든 순서). 회원은 비어 있다
 * @param superAdmin moa: 최고 관리자 역할을 가졌나 (2026-11-21)
 * @param protection moa: 요청한 사람이 이 계정을 다룰 수 없는 이유. 다룰 수 있으면 null (research R10)
 */
public record UserAdminRowDto(UserAdminDto user, List<RoleUserDto.RoleRef> accessRoles, boolean superAdmin,
	GrantBlock protection) {
}
