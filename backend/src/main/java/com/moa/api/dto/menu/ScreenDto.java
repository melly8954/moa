package com.moa.api.dto.menu;

import com.moa.api.screen.ScreenDefinition;
import com.moa.common.security.permission.PermissionDefinition;
import com.moa.common.security.permission.PermissionRegistry;

/**
 * 연결 화면과 그 권한 이름 (moa, specs/001-admin-menus FR-008). 화면은 권한 키 대신 이름을 보인다.
 *
 * @param permission 관리 연결 화면의 권한 키. 사용자 연결 화면은 null
 * @param permissionName 그 권한의 이름. 사용자 연결 화면은 null
 */
public record ScreenDto(String key, String name, String path, MenuAudience audience, String permission,
	String permissionName, boolean signInRequired) {

	public static ScreenDto of(ScreenDefinition screen, PermissionRegistry permissionRegistry) {
		String permissionName = permissionRegistry.find(screen.permission())
			.map(PermissionDefinition::name)
			.orElse(null);
		return new ScreenDto(screen.key(), screen.name(), screen.path(), screen.audience(),
			screen.permission(), permissionName, screen.signInRequired());
	}
}
