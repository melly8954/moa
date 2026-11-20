package com.moa.support;

import java.util.List;

import org.springframework.stereotype.Component;

import com.moa.api.screen.ScreenDefinition;
import com.moa.api.screen.ScreenSource;

/**
 * 테스트에서만 등록하는 연결 화면. 처음 등록하는 사용자 연결 화면(홈, 공지)은 모두 로그인이 필요 없어,
 * 로그인이 필요한 연결 화면의 공개 범위를 보려면 따로 하나 둔다 (specs/001-admin-menus research R17).
 */
@Component
public class TestScreens implements ScreenSource {

	public static final String MEMBER_ONLY = "TEST_MEMBER_ONLY";

	@Override
	public List<ScreenDefinition> screens() {
		return List.of(ScreenDefinition.user(MEMBER_ONLY, "시험 회원 화면", "/test-member-only", true));
	}
}
