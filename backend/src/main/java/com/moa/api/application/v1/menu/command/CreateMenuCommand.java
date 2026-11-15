package com.moa.api.application.v1.menu.command;

import com.moa.api.dto.menu.MenuAudience;
import com.moa.api.dto.menu.MenuVisibility;

/** programKey가 null이면 묶음 메뉴. audience는 1단계 메뉴만 쓴다 (하위 메뉴는 부모를 따른다) */
public record CreateMenuCommand(Long parentId, String name, String programKey, String icon, int sortOrder,
	MenuAudience audience, MenuVisibility visibility) {
}
