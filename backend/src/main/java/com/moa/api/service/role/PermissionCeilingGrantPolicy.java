package com.moa.api.service.role;

import java.util.Collection;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.moa.api.dto.role.AccessRoleDto;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.Role;
import com.moa.common.security.permission.PermissionGuard;
import com.moa.common.security.permission.PermissionRegistry;

import lombok.RequiredArgsConstructor;

/**
 * 기본 정책: 자기가 가진 권한까지만 다룬다 (선택 헬퍼 rbac). ADMIN은 제한이 없다.
 * 역할에 든 권한이 모두 내 권한 안에 있어야 만들고, 바꾸고, 지우고, 사용자에게 주거나 뺄 수 있다.
 * 코드에서 지워 등록되지 않은 키는 판정에서 무시한다.
 */
@Component
@RequiredArgsConstructor
public class PermissionCeilingGrantPolicy implements RoleGrantPolicy {

	private final PermissionGuard permissionGuard;
	private final PermissionRegistry permissionRegistry;

	@Override
	public boolean canGrant(AuthPrincipal actor, Collection<String> permissions) {
		if (actor.roles().contains(Role.ADMIN.name())) {
			return true;
		}
		Set<String> mine = permissionGuard.permissions(actor);
		return permissions.stream().filter(permissionRegistry::contains).allMatch(mine::contains);
	}

	@Override
	public boolean canManage(AuthPrincipal actor, AccessRoleDto role) {
		return canGrant(actor, role.permissions());
	}
}
