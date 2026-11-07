package com.moa.api.permission;

import java.util.List;

import org.springframework.stereotype.Component;

import com.moa.common.security.permission.PermissionDefinition;
import com.moa.common.security.permission.PermissionSource;

/** menu 헬퍼의 권한 */
@Component
public class MenuPermissions implements PermissionSource {

	public static final String MANAGE = "MENU_MANAGE";

	@Override
	public List<PermissionDefinition> permissions() {
		return List.of(new PermissionDefinition(MANAGE, "메뉴 관리", "메뉴"));
	}
}
