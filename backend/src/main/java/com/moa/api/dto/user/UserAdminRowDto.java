package com.moa.api.dto.user;

import java.util.List;

import com.moa.api.dto.role.RoleUserDto;

/**
 * 사용자 관리 목록·하나 조회의 한 줄 (moa, specs/002-admin-users FR-006). 업무 역할은 보기만 한다.
 *
 * @param accessRoles 받은 업무 역할 (만든 순서). ADMIN은 비어 있다 (모든 권한)
 */
public record UserAdminRowDto(UserAdminDto user, List<RoleUserDto.RoleRef> accessRoles) {
}
