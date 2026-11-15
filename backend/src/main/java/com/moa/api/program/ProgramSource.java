package com.moa.api.program;

import java.util.List;

/**
 * 프로그램을 등록한다 (moa, specs/001-admin-menus FR-008). 화면을 가진 헬퍼·기능이 자기 프로그램을 이 빈으로 내놓으면
 * ProgramRegistry가 모은다. 권한의 PermissionSource와 같은 모양이다.
 */
@FunctionalInterface
public interface ProgramSource {

	List<ProgramDefinition> programs();
}
