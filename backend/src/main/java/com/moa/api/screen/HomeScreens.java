package com.moa.api.screen;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 사용자 셸 첫 화면 (specs/001-admin-menus FR-008) */
@Component
@Order(5)
public class HomeScreens implements ScreenSource {

	@Override
	public List<ScreenDefinition> screens() {
		return List.of(ScreenDefinition.user("HOME", "홈", "/", false));
	}
}
