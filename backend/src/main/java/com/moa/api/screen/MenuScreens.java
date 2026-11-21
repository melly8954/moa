package com.moa.api.screen;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.moa.api.permission.MenuPermissions;

/** menu 헬퍼의 연결 화면 */
@Component
@Order(20)
public class MenuScreens implements ScreenSource {

	@Override
	public List<ScreenDefinition> screens() {
		return List.of(ScreenDefinition.admin("ADMIN_MENUS", "메뉴 관리", "/admin/menus", MenuPermissions.MANAGE));
	}
}
