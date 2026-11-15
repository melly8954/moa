package com.moa.api.program;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.moa.api.permission.NoticePermissions;

/** 공지사항의 프로그램 (specs/006-notices). 공지 목록·상세 API는 로그인 없이 부른다 */
@Component
@Order(60)
public class NoticePrograms implements ProgramSource {

	@Override
	public List<ProgramDefinition> programs() {
		return List.of(
			ProgramDefinition.admin("ADMIN_NOTICES", "공지 관리", "/admin/notices", NoticePermissions.MANAGE),
			ProgramDefinition.user("NOTICES", "공지", "/notices", false));
	}
}
