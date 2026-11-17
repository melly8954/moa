package com.moa.api.codegroup;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * local 프로필에서만 등록하는 시험 그룹 (moa, specs/004-admin-codes quickstart "화면 확인").
 * 아직 공통코드를 쓰는 기능이 없어 화면을 볼 수 없으므로 둔다. 실제 그룹은 쓰는 기능이 등록한다 (FR-002)
 */
@Component
@Profile("local")
public class LocalCodeGroups implements CodeGroupSource {

	@Override
	public List<CodeGroupDefinition> codeGroups() {
		return List.of(
			new CodeGroupDefinition("LOCAL_GAME_GENRE", "게임 장르 (시험)", "화면 확인용 시험 그룹", "local 프로필 화면 확인"),
			new CodeGroupDefinition("LOCAL_REPORT_REASON", "신고 사유 (시험)", "화면 확인용 시험 그룹", "local 프로필 화면 확인"));
	}
}
