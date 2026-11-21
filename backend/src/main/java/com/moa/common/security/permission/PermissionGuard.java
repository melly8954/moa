package com.moa.common.security.permission;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.Role;

import lombok.RequiredArgsConstructor;

/**
 * 권한 판정. 빈 이름 permissionGuard는 @RequirePermission의 식이 부른다.
 *
 * moa: ADMIN은 "모든 권한"이 아니라 관리자 계정이다 (specs/003-admin-roles FR-002·FR-010, research R16).
 * <ul>
 *   <li>USER(회원): 관리 권한이 없다</li>
 *   <li>ADMIN이고 PermissionChecker가 모든 권한이라고 하면(최고 관리자 역할): 등록된 모든 권한</li>
 *   <li>그 밖의 ADMIN: PermissionChecker가 준 권한 중 등록된 것만</li>
 *   <li>PermissionChecker가 없으면(rbac 헬퍼를 쓰지 않으면) ADMIN이 모든 권한을 갖는다 (키트 기본)</li>
 * </ul>
 * 키트로 옮길 것 (키트 골격의 판정과 다르다)
 */
@Component("permissionGuard")
@RequiredArgsConstructor
public class PermissionGuard {

	private final PermissionRegistry registry;
	private final ObjectProvider<PermissionChecker> checker;

	/** @RequirePermission이 부른다. 로그인하지 않았으면 false (A001로 응답된다) */
	public boolean has(Authentication authentication, String permission) {
		return authentication != null && authentication.getPrincipal() instanceof AuthPrincipal principal
			&& has(principal, permission);
	}

	public boolean has(AuthPrincipal principal, String permission) {
		return permissions(principal).contains(permission);
	}

	/** moa: 등록된 모든 권한을 갖는가 (최고 관리자, rbac가 없으면 ADMIN) */
	public boolean hasAll(AuthPrincipal principal) {
		if (!principal.roles().contains(Role.ADMIN.name())) {
			return false;
		}
		PermissionChecker found = checker.getIfAvailable();
		return found == null || found.hasAllPermissions(principal.userId());
	}

	/** 이 사용자의 권한 키. 등록 순서를 지킨다 */
	public Set<String> permissions(AuthPrincipal principal) {
		if (!principal.roles().contains(Role.ADMIN.name())) {
			return Set.of();
		}
		if (hasAll(principal)) {
			return registry.keys();
		}
		Set<String> granted = checker.getObject().permissions(principal.userId());
		return registry.keys().stream()
			.filter(granted::contains)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}
}
