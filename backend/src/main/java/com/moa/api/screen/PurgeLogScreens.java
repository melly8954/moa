package com.moa.api.screen;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.moa.api.permission.PurgeLogPermissions;

/** 개인정보 파기 이력의 연결 화면 (moa, specs/013-purge-log FR-007). 접속 이력 다음 */
@Component
@Order(45)
public class PurgeLogScreens implements ScreenSource {

	@Override
	public List<ScreenDefinition> screens() {
		return List.of(ScreenDefinition.admin("ADMIN_PURGE_LOGS", "파기 이력", "/admin/purge-logs",
			PurgeLogPermissions.READ));
	}
}
