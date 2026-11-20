package com.moa.api.screen;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.moa.api.permission.UserPermissions;

/** user-admin 헬퍼의 연결 화면 */
@Component
@Order(10)
public class UserScreens implements ScreenSource {

	@Override
	public List<ScreenDefinition> screens() {
		return List.of(ScreenDefinition.admin("ADMIN_USERS", "사용자 관리", "/admin/users", UserPermissions.MANAGE));
	}
}
