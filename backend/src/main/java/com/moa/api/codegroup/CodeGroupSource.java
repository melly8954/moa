package com.moa.api.codegroup;

import java.util.List;

/**
 * 코드 그룹을 등록한다 (moa, specs/004-admin-codes FR-002·FR-004). 공통코드를 쓰는 기능이 자기 그룹을 이 빈으로 내놓으면
 * CodeGroupRegistry가 모은다. 프로그램의 ProgramSource와 같은 모양이다.
 */
@FunctionalInterface
public interface CodeGroupSource {

	List<CodeGroupDefinition> codeGroups();
}
