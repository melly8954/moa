package com.moa.api.program;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.moa.api.permission.MenuPermissions;

/** menu 헬퍼의 프로그램 */
@Component
@Order(20)
public class MenuPrograms implements ProgramSource {

	@Override
	public List<ProgramDefinition> programs() {
		return List.of(ProgramDefinition.admin("ADMIN_MENUS", "메뉴 관리", "/admin/menus", MenuPermissions.MANAGE));
	}
}
