package com.moa.api.screen;

import java.util.List;

/**
 * 연결 화면을 등록한다 (moa, specs/001-admin-menus FR-008). 화면을 가진 헬퍼·기능이 자기 연결 화면을 이 빈으로 내놓으면
 * ScreenRegistry가 모은다. 권한의 PermissionSource와 같은 모양이다.
 */
@FunctionalInterface
public interface ScreenSource {

	List<ScreenDefinition> screens();
}
