package com.moa.api.dto.role;

import java.util.List;

/**
 * 화면에 보일 역할 (moa, specs/003-admin-roles research R18). 역할에 이 사람이 다룰 수 있는지와 권한 이름을 붙인다.
 *
 * @param permissionNames 역할에 담긴 권한의 이름 (사용자 관리 드로어가 역할마다 쓰는 메뉴로 보인다). 최고 관리자 역할은 빈다
 * @param grantable 이 사람이 이 역할을 다루고 주고 뺄 수 있나
 * @param blockReason 다룰 수 없는 이유. 다룰 수 있으면 null
 */
public record AccessRoleView(AccessRoleDto role, List<String> permissionNames, boolean grantable,
	GrantBlock blockReason) {
}
