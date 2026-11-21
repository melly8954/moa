package com.moa.common.security;

/**
 * 시스템 역할(화면 이름 "계정 종류"). moa: USER는 회원, ADMIN은 관리자 계정이다 (specs/002-admin-users FR-005).
 * ADMIN이 무엇을 할 수 있는지는 rbac 역할이 정하고, 모든 권한은 최고 관리자 역할이 가진다 (PermissionGuard,
 * specs/003-admin-roles FR-010). 업무 역할(매니저, 상담원 등)은 이 enum에 더하지 않는다. rbac 헬퍼로 DB에서 만들고 권한을 묶는다.
 */
public enum Role {
	USER,
	ADMIN
}
