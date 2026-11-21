package com.moa.api.screen;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.moa.api.permission.CodePermissions;

/** code 헬퍼의 연결 화면 */
@Component
@Order(30)
public class CodeScreens implements ScreenSource {

	@Override
	public List<ScreenDefinition> screens() {
		return List.of(ScreenDefinition.admin("ADMIN_CODES", "코드 관리", "/admin/codes", CodePermissions.MANAGE));
	}
}
