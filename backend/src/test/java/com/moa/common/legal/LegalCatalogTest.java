package com.moa.common.legal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * 공개 문서 문안의 시작 검사 (specs/012-terms-privacy FR-008, research R6).
 * 실제 문안은 배포 jar와 같은 리소스 패턴(classpath*:legal/*\/*.json)으로 읽는다.
 */
class LegalCatalogTest {

	private static final PathMatchingResourcePatternResolver RESOLVER = new PathMatchingResourcePatternResolver();
	private static final LocalDate TODAY = LocalDate.of(2026, 11, 28);

	@Test
	@DisplayName("FR-008 실제 문안(legal/)은 검사를 통과하고 문서마다 장이 있다")
	void realDocumentsPass() {
		LegalCatalog catalog = LegalCatalog.load(RESOLVER, "classpath:legal/", LocalDate.now(ZoneId.of("Asia/Seoul")),
			"draft");

		for (LegalDocument document : LegalDocument.values()) {
			assertThat(catalog.versions(document)).isNotEmpty();
			assertThat(catalog.versions(document)).allSatisfy(version -> assertThat(version.sections()).isNotEmpty());
		}
		assertThat(catalog.versions(LegalDocument.TERMS)).extracting(LegalVersion::version).contains("draft");
	}

	@Test
	@DisplayName("장은 section id, h2 제목, h2를 뺀 본문으로 나뉘고 버전은 시행일 최근순이다")
	void splitsSections() {
		LegalCatalog catalog = LegalCatalog.load(RESOLVER, "classpath:legal-test/", TODAY, "draft");

		assertThat(catalog.versions(LegalDocument.PRIVACY)).extracting(LegalVersion::version)
			.containsExactly("next", "draft", "old");
		LegalVersion draft = catalog.versions(LegalDocument.PRIVACY).get(1);
		assertThat(draft.sections()).extracting(LegalSection::id).containsExactly("first", "retention");
		assertThat(draft.sections().get(1).heading()).isEqualTo("보유 기간");
		assertThat(draft.sections().get(1).html()).contains("<table>").doesNotContain("<h2>");
	}

	@ParameterizedTest(name = "FR-008 {0}이면 시작하지 않는다")
	@CsvSource({
		"script, 본문에 스크립트",
		"missing-html, 짝 본문 파일(.html)이 없습니다",
		"duplicate-date, 시행일이 겹칩니다",
		"no-effective, 시행된 버전",
		"outside-text, 장 밖에 글이 있습니다",
		"no-service-version, 가입 동의 버전",
	})
	void invalidDocumentsFail(String folder, String message) {
		assertThatThrownBy(() -> LegalCatalog.load(RESOLVER, "classpath:legal-invalid/" + folder + "/", TODAY, "draft"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining(message);
	}
}
