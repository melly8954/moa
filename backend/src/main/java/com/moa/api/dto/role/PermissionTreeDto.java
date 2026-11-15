package com.moa.api.dto.role;

import java.util.List;

/**
 * 역할 창에서 권한을 고르는 트리 (moa, specs/003-admin-roles FR-004).
 * 관리 메뉴 트리(꺼진 메뉴 포함)와, 어느 관리 메뉴도 가리키지 않는 관리 프로그램이다.
 */
public record PermissionTreeDto(List<Node> menus, List<Unlisted> unlisted) {

	/**
	 * 관리 메뉴 하나. 묶음 메뉴는 권한이 없고 children이 있다.
	 *
	 * @param permission 화면 메뉴가 가리키는 프로그램의 권한 키. 묶음 메뉴는 null
	 */
	public record Node(Long menuId, String name, boolean enabled, String permission, String permissionName,
		List<Node> children) {
	}

	/** 메뉴에 없는 관리 프로그램 */
	public record Unlisted(String programKey, String name, String permission, String permissionName) {
	}
}
