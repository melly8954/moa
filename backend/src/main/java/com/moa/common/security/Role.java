package com.moa.common.security;

/**
 * 시스템 역할. USER는 로그인한 사용자, ADMIN은 모든 권한을 가진 관리자다 (PermissionGuard).
 * 업무 역할(매니저, 상담원 등)은 이 enum에 더하지 않는다. rbac 헬퍼로 DB에서 만들고 권한을 묶는다.
 */
public enum Role {
	USER,
	ADMIN
}
