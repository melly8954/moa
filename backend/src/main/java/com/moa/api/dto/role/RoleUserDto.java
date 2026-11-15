package com.moa.api.dto.role;

import java.util.List;

import com.moa.common.security.Role;

/**
 * 역할 관리에서 보는 사용자 (moa, specs/003-admin-roles FR-005·FR-006). 역할 관리에 필요한 것만 담는다.
 *
 * @param role 시스템 역할
 * @param accessRoles 받은 업무 역할 (만든 순서)
 */
public record RoleUserDto(Long id, String loginId, String name, Role role, List<RoleRef> accessRoles) {

	public record RoleRef(Long id, String code, String name) {
	}
}
