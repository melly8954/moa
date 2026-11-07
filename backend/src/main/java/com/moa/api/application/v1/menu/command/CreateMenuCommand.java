package com.moa.api.application.v1.menu.command;

/** permission이 null이면 로그인한 사용자 모두가 본다 */
public record CreateMenuCommand(Long parentId, String name, String path, String icon, int sortOrder,
	String permission) {
}
