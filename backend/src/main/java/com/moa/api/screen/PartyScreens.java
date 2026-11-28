package com.moa.api.screen;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 파티의 연결 화면 (specs/008-party FR-030, research R11). 모집 페이지 목록은 로그인 없이 부르고, 내 파티는 회원만 부른다
 */
@Component
@Order(80)
public class PartyScreens implements ScreenSource {

	@Override
	public List<ScreenDefinition> screens() {
		return List.of(
			ScreenDefinition.user("RECRUIT", "모집", "/recruit", false),
			ScreenDefinition.user("MY_PARTY", "내 파티", "/my-party", true));
	}
}
