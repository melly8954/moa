package com.moa.api.application.v1.menu.command;

import com.moa.api.dto.menu.MenuAudience;
import com.moa.api.dto.menu.MenuVisibility;

/** screenKey가 null이면 묶음 메뉴. audience가 null이면 그대로 둔다 */
public record ModifyMenuCommand(Long id, Long parentId, String name, String screenKey, String icon, int sortOrder,
	boolean enabled, MenuAudience audience, MenuVisibility visibility) {
}
