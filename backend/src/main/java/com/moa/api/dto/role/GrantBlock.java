package com.moa.api.dto.role;

/**
 * 이 사람이 이 역할(또는 계정)을 다룰 수 없는 이유 (moa, specs/003-admin-roles research R18, specs/002-admin-users research R10).
 * 화면은 이유만 받아 체크 칸·단추를 끄고 안내한다.
 */
public enum GrantBlock {
	/** 최고 관리자만 다룬다 (최고 관리자 역할, 사용자 관리 권한이 든 역할, 최고 관리자 계정) */
	SUPER_ADMIN_ONLY,
	/** 내가 갖지 않은 권한이 들어 있다 */
	WIDER_PERMISSIONS
}
