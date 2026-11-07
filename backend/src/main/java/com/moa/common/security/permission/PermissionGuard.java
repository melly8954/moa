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
 * 권한 판정. ADMIN은 등록된 모든 권한을 갖는다. 다른 사용자는 PermissionChecker가 준 권한 중 등록된 것만 갖는다.
 * 빈 이름 permissionGuard는 @RequirePermission의 식이 부른다.
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

	/** 이 사용자의 권한 키. 등록 순서를 지킨다 */
	public Set<String> permissions(AuthPrincipal principal) {
		if (principal.roles().contains(Role.ADMIN.name())) {
			return registry.keys();
		}
		PermissionChecker found = checker.getIfAvailable();
		if (found == null) {
			return Set.of();
		}
		Set<String> granted = found.permissions(principal.userId());
		return registry.keys().stream()
			.filter(granted::contains)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}
}
