package com.moa.api.dto.menu;

import java.util.List;

import com.moa.api.entity.menu.Menu;

/**
 * 메뉴와 자식 메뉴. 1단계 메뉴만 children이 있다.
 *
 * @param programKey 가리키는 프로그램 키. 없으면 묶음 메뉴
 * @param program 등록된 프로그램. 묶음 메뉴이거나 없는 프로그램이면 null
 */
public record MenuDto(Long id, Long parentId, String name, String programKey, ProgramDto program,
	String icon, int sortOrder, boolean enabled, MenuAudience audience, MenuVisibility visibility,
	List<MenuDto> children) {

	public static MenuDto of(Menu menu, ProgramDto program, List<MenuDto> children) {
		return new MenuDto(menu.getId(), menu.getParentId(), menu.getName(), menu.getProgramKey(), program,
			menu.getIcon(), menu.getSortOrder(), menu.isEnabled(), menu.getAudience(), menu.getVisibility(),
			children);
	}

	/** 화면 경로 (프로그램의 경로). 묶음 메뉴와 없는 프로그램은 null */
	public String path() {
		return program == null ? null : program.path();
	}

	/** 프로그램 키가 있는데 등록된 프로그램이 없다 (코드에서 빠졌다) */
	public boolean missingProgram() {
		return programKey != null && program == null;
	}
}
