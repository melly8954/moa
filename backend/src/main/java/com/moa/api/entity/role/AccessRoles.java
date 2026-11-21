package com.moa.api.entity.role;

/**
 * 코드에 고정된 역할 (moa, specs/003-admin-roles FR-010, research R16).
 * 최고 관리자 역할은 tb_access_role의 행 하나이고 권한 행을 두지 않는다. 모든 권한은 PermissionGuard가 준다.
 */
public final class AccessRoles {

	/** 최고 관리자 역할의 코드. V7 마이그레이션이 행을 넣는다 */
	public static final String SUPER_ADMIN_CODE = "SUPER_ADMIN";

	public static final String SUPER_ADMIN_NAME = "최고 관리자";

	public static final String SUPER_ADMIN_DESCRIPTION = "모든 관리 메뉴와 기능을 써요";

	private AccessRoles() {
	}

	public static boolean isSuperAdmin(String code) {
		return SUPER_ADMIN_CODE.equals(code);
	}
}
