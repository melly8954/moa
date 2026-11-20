package com.moa.api.screen;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.moa.api.permission.LoginHistoryPermissions;

/** login-history 헬퍼의 연결 화면 */
@Component
@Order(40)
public class LoginHistoryScreens implements ScreenSource {

	@Override
	public List<ScreenDefinition> screens() {
		return List.of(ScreenDefinition.admin("ADMIN_LOGIN_HISTORIES", "접속 이력", "/admin/login-histories",
			LoginHistoryPermissions.READ));
	}
}
