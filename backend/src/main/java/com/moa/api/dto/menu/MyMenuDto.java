package com.moa.api.dto.menu;

import java.util.List;
import java.util.Set;

/**
 * 내 메뉴 한 줄 (moa, specs/001-admin-menus FR-002, 2026-11-22). 관리자 계정은 사용 중인 관리 메뉴를 모두 받고,
 * 연결 화면의 권한이 있는지를 accessible로 받는다. 상위 메뉴는 하위 메뉴 중 하나라도 accessible이면 accessible이다.
 *
 * @param path 화면 경로. 상위 메뉴(연결 화면 없음)면 null
 */
public record MyMenuDto(Long id, String name, String path, String icon, boolean accessible,
	List<MyMenuDto> children) {

	public static MyMenuDto of(MenuDto menu, Set<String> permissions) {
		List<MyMenuDto> children = menu.children().stream().map(child -> of(child, permissions)).toList();
		boolean accessible = menu.screen() != null
			? permissions.contains(menu.screen().permission())
			: children.stream().anyMatch(MyMenuDto::accessible);
		return new MyMenuDto(menu.id(), menu.name(), menu.path(), menu.icon(), accessible, children);
	}

	/** 사용자 메뉴: 공개 범위로 이미 걸렀으므로 모두 들어갈 수 있다 */
	public static MyMenuDto open(MenuDto menu) {
		return new MyMenuDto(menu.id(), menu.name(), menu.path(), menu.icon(), true,
			menu.children().stream().map(MyMenuDto::open).toList());
	}
}
