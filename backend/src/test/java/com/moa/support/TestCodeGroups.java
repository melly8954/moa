package com.moa.support;

import java.util.List;

import org.springframework.stereotype.Component;

import com.moa.api.codegroup.CodeGroupDefinition;
import com.moa.api.codegroup.CodeGroupSource;

/**
 * 테스트에서만 등록하는 코드 그룹 둘 (specs/004-admin-codes research R8). moa에는 아직 공통코드를 쓰는 기능이 없다
 */
@Component
public class TestCodeGroups implements CodeGroupSource {

	public static final String GENRE = "TEST_GENRE";
	public static final String REASON = "TEST_REASON";

	@Override
	public List<CodeGroupDefinition> codeGroups() {
		return List.of(
			new CodeGroupDefinition(GENRE, "시험 장르", "게임 장르", "시험 게임 화면"),
			new CodeGroupDefinition(REASON, "시험 사유", null, "시험 신고 화면"));
	}
}
