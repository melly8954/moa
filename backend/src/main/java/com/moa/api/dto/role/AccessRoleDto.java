package com.moa.api.dto.role;

import java.util.List;

import com.moa.api.entity.role.AccessRole;
import com.moa.api.entity.role.AccessRoles;

/**
 * 업무 역할. 권한 키는 이름순이다.
 *
 * @param userCount 이 역할을 가진 사용자 수
 * @param fixed moa: 코드에 고정된 최고 관리자 역할인가 (specs/003-admin-roles FR-010). 권한 행이 없고 모든 권한을 갖는다
 */
public record AccessRoleDto(Long id, String code, String name, String description, List<String> permissions,
	long userCount, boolean fixed) {

	public static AccessRoleDto of(AccessRole role, long userCount) {
		return new AccessRoleDto(role.getId(), role.getCode(), role.getName(), role.getDescription(),
			role.getPermissions().stream().sorted().toList(), userCount, AccessRoles.isSuperAdmin(role.getCode()));
	}
}
