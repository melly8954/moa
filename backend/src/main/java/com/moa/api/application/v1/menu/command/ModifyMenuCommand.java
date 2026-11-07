package com.moa.api.application.v1.menu.command;

/** permission이 null이면 로그인한 사용자 모두가 본다 */
public record ModifyMenuCommand(Long id, Long parentId, String name, String path, String icon, int sortOrder,
	boolean enabled, String permission) {
}
