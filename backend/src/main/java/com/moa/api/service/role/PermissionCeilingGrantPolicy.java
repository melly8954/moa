package com.moa.api.service.role;

import java.util.Collection;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.moa.api.dto.role.AccessRoleDto;
import com.moa.api.dto.role.GrantBlock;
import com.moa.api.permission.UserPermissions;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.permission.PermissionGuard;
import com.moa.common.security.permission.PermissionRegistry;

import lombok.RequiredArgsConstructor;

/**
 * 기본 정책: 자기가 가진 권한까지만 다룬다 (선택 헬퍼 rbac). 모든 권한을 가진 사용자는 제한이 없다.
 * 역할에 든 권한이 모두 내 권한 안에 있어야 만들고, 바꾸고, 지우고, 사용자에게 주거나 뺄 수 있다.
 * 코드에서 지워 등록되지 않은 키는 판정에서 무시한다.
 *
 * moa (specs/003-admin-roles FR-008·FR-010, research R17): 최고 관리자 역할과 사용자 관리 권한이 든 역할은
 * 최고 관리자만 다룬다. 사용자 관리 권한은 계정을 만들고 비밀번호를 초기화할 수 있어 권한을 넓히는 길이 되기 때문이다
 */
@Component
@RequiredArgsConstructor
public class PermissionCeilingGrantPolicy implements RoleGrantPolicy {

	private final PermissionGuard permissionGuard;
	private final PermissionRegistry permissionRegistry;

	@Override
	public boolean canGrant(AuthPrincipal actor, Collection<String> permissions) {
		if (permissionGuard.hasAll(actor)) {
			return true;
		}
		if (permissions.contains(UserPermissions.MANAGE)) {
			return false;
		}
		Set<String> mine = permissionGuard.permissions(actor);
		return permissions.stream().filter(permissionRegistry::contains).allMatch(mine::contains);
	}

	@Override
	public boolean canManage(AuthPrincipal actor, AccessRoleDto role) {
		if (role.fixed()) {
			return permissionGuard.hasAll(actor);
		}
		return canGrant(actor, role.permissions());
	}

	@Override
	public GrantBlock blockReason(AuthPrincipal actor, AccessRoleDto role) {
		if (canManage(actor, role)) {
			return null;
		}
		return role.fixed() || role.permissions().contains(UserPermissions.MANAGE) ? GrantBlock.SUPER_ADMIN_ONLY
			: GrantBlock.WIDER_PERMISSIONS;
	}
}
