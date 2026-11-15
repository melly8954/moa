package com.moa.api.program;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 사용자 셸 첫 화면 (specs/001-admin-menus FR-008) */
@Component
@Order(5)
public class HomePrograms implements ProgramSource {

	@Override
	public List<ProgramDefinition> programs() {
		return List.of(ProgramDefinition.user("HOME", "홈", "/", false));
	}
}
