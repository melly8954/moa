package com.moa.api.program;

import com.moa.api.dto.menu.MenuAudience;

/**
 * 프로그램 하나. 메뉴에 걸 수 있는 화면이다 (moa, specs/001-admin-menus FR-008).
 * 관리 프로그램은 권한 하나를 갖고, 사용자 프로그램은 로그인 필요 여부를 갖는다.
 * 권한과 로그인 필요 여부는 그 화면이 쓰는 API가 실제로 막는 조건과 같아야 한다.
 *
 * @param key 대문자·숫자·밑줄 (예: ADMIN_CODES)
 * @param name 화면에 보일 이름
 * @param path 화면 경로 (/로 시작)
 * @param audience 대상. 메뉴는 같은 대상의 프로그램만 가리킨다
 * @param permission 관리 프로그램의 권한 키. 사용자 프로그램은 null
 * @param signInRequired 로그인이 필요한가. 관리 프로그램은 언제나 true
 */
public record ProgramDefinition(String key, String name, String path, MenuAudience audience, String permission,
	boolean signInRequired) {

	public static ProgramDefinition admin(String key, String name, String path, String permission) {
		return new ProgramDefinition(key, name, path, MenuAudience.ADMIN, permission, true);
	}

	public static ProgramDefinition user(String key, String name, String path, boolean signInRequired) {
		return new ProgramDefinition(key, name, path, MenuAudience.USER, null, signInRequired);
	}
}
