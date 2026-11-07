package com.moa.api.permission;

import java.util.List;

import org.springframework.stereotype.Component;

import com.moa.common.security.permission.PermissionDefinition;
import com.moa.common.security.permission.PermissionSource;

/** user-admin 헬퍼의 권한 */
@Component
public class UserPermissions implements PermissionSource {

	public static final String MANAGE = "USER_MANAGE";

	@Override
	public List<PermissionDefinition> permissions() {
		return List.of(new PermissionDefinition(MANAGE, "사용자 관리", "사용자"));
	}
}
