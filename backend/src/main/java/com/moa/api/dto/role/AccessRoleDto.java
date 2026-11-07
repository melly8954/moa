package com.moa.api.dto.role;

import java.util.List;

import com.moa.api.entity.role.AccessRole;

/**
 * 업무 역할. 권한 키는 이름순이다.
 *
 * @param userCount 이 역할을 가진 사용자 수
 */
public record AccessRoleDto(Long id, String code, String name, String description, List<String> permissions,
	long userCount) {

	public static AccessRoleDto of(AccessRole role, long userCount) {
		return new AccessRoleDto(role.getId(), role.getCode(), role.getName(), role.getDescription(),
			role.getPermissions().stream().sorted().toList(), userCount);
	}
}
