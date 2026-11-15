package com.moa.api.dto.menu;

import com.moa.api.program.ProgramDefinition;
import com.moa.common.security.permission.PermissionDefinition;
import com.moa.common.security.permission.PermissionRegistry;

/**
 * 프로그램과 그 권한 이름 (moa, specs/001-admin-menus FR-008). 화면은 권한 키 대신 이름을 보인다.
 *
 * @param permission 관리 프로그램의 권한 키. 사용자 프로그램은 null
 * @param permissionName 그 권한의 이름. 사용자 프로그램은 null
 */
public record ProgramDto(String key, String name, String path, MenuAudience audience, String permission,
	String permissionName, boolean signInRequired) {

	public static ProgramDto of(ProgramDefinition program, PermissionRegistry permissionRegistry) {
		String permissionName = permissionRegistry.find(program.permission())
			.map(PermissionDefinition::name)
			.orElse(null);
		return new ProgramDto(program.key(), program.name(), program.path(), program.audience(),
			program.permission(), permissionName, program.signInRequired());
	}
}
