package com.moa.api.dto.menu;

/**
 * 메뉴 대상 (moa, specs/001-admin-menus FR-007). ADMIN은 관리 셸, USER는 사용자 셸에 보인다.
 * 하위 메뉴는 부모와 같은 대상이다.
 */
public enum MenuAudience {
	ADMIN,
	USER
}
