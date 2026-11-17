package com.moa.api.codegroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 코드 그룹 등록 검사 (specs/004-admin-codes FR-004, research R5). DB가 필요 없다
 */
class CodeGroupRegistryTest {

	@Test
	@DisplayName("US1-AC6 등록된 그룹이 없으면 그룹 목록이 비어 있다 (화면은 빈 안내를 보인다)")
	void emptyWhenNothingRegistered() {
		assertThat(new CodeGroupRegistry(List.of()).all()).isEmpty();
	}

	@Test
	@DisplayName("그룹 코드 형식·겹침·이름·쓰는 곳이 틀리면 시작이 실패한다")
	void rejectsInvalidGroups() {
		assertThatThrownBy(() -> registry(new CodeGroupDefinition("game_genre", "장르", null, "게임")))
			.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> registry(new CodeGroupDefinition("GENRE", "", null, "게임")))
			.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> registry(new CodeGroupDefinition("GENRE", "장르", null, " ")))
			.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> registry(new CodeGroupDefinition("GENRE", "장르", null, "게임"),
			new CodeGroupDefinition("GENRE", "장르 2", null, "게임"))).isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("등록 순서대로 보이고 그룹 코드로 찾는다")
	void keepsOrder() {
		CodeGroupRegistry registry = registry(new CodeGroupDefinition("B_GROUP", "나", null, "화면"),
			new CodeGroupDefinition("A_GROUP", "가", "설명", "화면"));

		assertThat(registry.all()).extracting(CodeGroupDefinition::groupCode).containsExactly("B_GROUP", "A_GROUP");
		assertThat(registry.contains("A_GROUP")).isTrue();
		assertThat(registry.contains("NONE")).isFalse();
	}

	private static CodeGroupRegistry registry(CodeGroupDefinition... groups) {
		return new CodeGroupRegistry(List.of(groups));
	}
}
