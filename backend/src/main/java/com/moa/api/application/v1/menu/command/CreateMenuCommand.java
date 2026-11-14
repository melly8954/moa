package com.moa.api.application.v1.menu.command;

import com.moa.api.dto.menu.MenuAudience;

/** permission이 null이면 대상의 모두가 본다. audience는 1단계 메뉴만 쓴다 (하위 메뉴는 부모를 따른다) */
public record CreateMenuCommand(Long parentId, String name, String path, String icon, int sortOrder,
	String permission, MenuAudience audience) {
}
