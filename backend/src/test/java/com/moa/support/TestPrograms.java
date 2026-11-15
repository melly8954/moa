package com.moa.support;

import java.util.List;

import org.springframework.stereotype.Component;

import com.moa.api.program.ProgramDefinition;
import com.moa.api.program.ProgramSource;

/**
 * 테스트에서만 등록하는 프로그램. 처음 등록하는 사용자 프로그램(홈, 공지)은 모두 로그인이 필요 없어,
 * 로그인이 필요한 프로그램의 공개 범위를 보려면 따로 하나 둔다 (specs/001-admin-menus research R17).
 */
@Component
public class TestPrograms implements ProgramSource {

	public static final String MEMBER_ONLY = "TEST_MEMBER_ONLY";

	@Override
	public List<ProgramDefinition> programs() {
		return List.of(ProgramDefinition.user(MEMBER_ONLY, "시험 회원 화면", "/test-member-only", true));
	}
}
