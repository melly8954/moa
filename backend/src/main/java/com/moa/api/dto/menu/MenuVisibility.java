package com.moa.api.dto.menu;

/**
 * 사용자 메뉴의 공개 범위 (moa, specs/001-admin-menus FR-007). 사이드바에 보일지만 정한다.
 * 화면과 API에 들어올 수 있는지는 코드(API의 권한·공개 표시, 로그인이 필요한 화면 묶음)가 정한다.
 */
public enum MenuVisibility {
	/** 로그인하지 않은 사람을 포함한 모두 */
	PUBLIC,
	/** 로그인한 사용자만 */
	MEMBER
}
