package com.moa.api.screen;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.moa.api.permission.RolePermissions;

/** rbac 헬퍼의 연결 화면 */
@Component
@Order(50)
public class RoleScreens implements ScreenSource {

	@Override
	public List<ScreenDefinition> screens() {
		return List.of(ScreenDefinition.admin("ADMIN_ROLES", "역할 관리", "/admin/roles", RolePermissions.MANAGE));
	}
}
