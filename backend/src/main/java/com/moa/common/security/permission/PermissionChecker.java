package com.moa.common.security.permission;

import java.util.Set;

/**
 * 관리자 계정(ADMIN)에게 준 권한을 읽는다. rbac 헬퍼가 구현해 빈으로 등록한다.
 * 구현이 없으면 ADMIN이 모든 권한을 갖는다. 회원(USER)은 구현과 관계없이 관리 권한이 없다 (PermissionGuard).
 */
public interface PermissionChecker {

	/** 이 사용자에게 준 권한 키 */
	Set<String> permissions(Long userId);

	/** moa: 등록된 모든 권한을 갖는가 (최고 관리자 역할, specs/003-admin-roles FR-010). 기본은 아니다 */
	default boolean hasAllPermissions(Long userId) {
		return false;
	}
}
