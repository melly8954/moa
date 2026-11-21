package com.moa.api.screen;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.moa.api.permission.NoticePermissions;

/** 공지사항의 연결 화면 (specs/006-notices). 공지 목록·상세 API는 로그인 없이 부른다 */
@Component
@Order(60)
public class NoticeScreens implements ScreenSource {

	@Override
	public List<ScreenDefinition> screens() {
		return List.of(
			ScreenDefinition.admin("ADMIN_NOTICES", "공지 관리", "/admin/notices", NoticePermissions.MANAGE),
			ScreenDefinition.user("NOTICES", "공지", "/notices", false));
	}
}
