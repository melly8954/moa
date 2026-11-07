package com.moa.api.dto.menu;

import java.util.List;

import com.moa.api.entity.menu.Menu;

/**
 * 메뉴와 자식 메뉴. 1단계 메뉴만 children이 있다.
 */
public record MenuDto(Long id, Long parentId, String name, String path, String icon, int sortOrder,
	boolean enabled, String permission, List<MenuDto> children) {

	public static MenuDto of(Menu menu, List<MenuDto> children) {
		return new MenuDto(menu.getId(), menu.getParentId(), menu.getName(), menu.getPath(), menu.getIcon(),
			menu.getSortOrder(), menu.isEnabled(), menu.getPermission(), children);
	}
}
