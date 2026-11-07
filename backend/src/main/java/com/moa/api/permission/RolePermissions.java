package com.moa.api.permission;

import java.util.List;

import org.springframework.stereotype.Component;

import com.moa.common.security.permission.PermissionDefinition;
import com.moa.common.security.permission.PermissionSource;

/** rbac 헬퍼의 권한 */
@Component
public class RolePermissions implements PermissionSource {

	public static final String MANAGE = "ROLE_MANAGE";

	@Override
	public List<PermissionDefinition> permissions() {
		return List.of(new PermissionDefinition(MANAGE, "역할 관리", "역할"));
	}
}
