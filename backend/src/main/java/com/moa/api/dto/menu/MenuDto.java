package com.moa.api.dto.menu;

import java.util.List;

import com.moa.api.entity.menu.Menu;

/**
 * 메뉴와 자식 메뉴. 1단계 메뉴만 children이 있다.
 *
 * @param screenKey 가리키는 연결 화면 키. 없으면 묶음 메뉴
 * @param screen 등록된 연결 화면. 묶음 메뉴이거나 없는 연결 화면이면 null
 */
public record MenuDto(Long id, Long parentId, String name, String screenKey, ScreenDto screen,
	String icon, int sortOrder, boolean enabled, MenuAudience audience, MenuVisibility visibility,
	List<MenuDto> children) {

	public static MenuDto of(Menu menu, ScreenDto screen, List<MenuDto> children) {
		return new MenuDto(menu.getId(), menu.getParentId(), menu.getName(), menu.getScreenKey(), screen,
			menu.getIcon(), menu.getSortOrder(), menu.isEnabled(), menu.getAudience(), menu.getVisibility(),
			children);
	}

	/** 화면 경로 (연결 화면의 경로). 묶음 메뉴와 없는 연결 화면은 null */
	public String path() {
		return screen == null ? null : screen.path();
	}

	/** 연결 화면 키가 있는데 등록된 연결 화면이 없다 (코드에서 빠졌다) */
	public boolean missingScreen() {
		return screenKey != null && screen == null;
	}
}
