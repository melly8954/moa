package com.moa.api.service.legal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.moa.api.dto.legal.LegalDocumentDto;
import com.moa.api.dto.legal.LegalVersionDto;
import com.moa.api.dto.legal.VersionState;
import com.moa.api.repository.reader.legal.LegalReader;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.legal.LegalCatalog;

/**
 * 지금·예정·예전 버전 고르기 (specs/012-terms-privacy research R5). 테스트용 문안은 old 2000-01-01, draft 2020-01-01,
 * next 2099-01-01이다.
 */
class LegalServiceTest {

	private static final LegalCatalog CATALOG = LegalCatalog.load(new PathMatchingResourcePatternResolver(),
		"classpath:legal-test/", LocalDate.of(2026, 11, 28), "draft");

	private final LegalService service = new LegalService(new LegalReader(CATALOG));

	@Test
	@DisplayName("시행일이 오늘 이전 중 가장 늦은 것이 지금 버전, 그 뒤는 예정, 나머지는 예전이다")
	void picksStates() {
		LegalDocumentDto document = service.document("privacy", LocalDate.of(2026, 11, 28));

		assertThat(document.current().version().version()).isEqualTo("draft");
		assertThat(document.upcoming()).isNotNull();
		assertThat(document.upcoming().version().version()).isEqualTo("next");
		assertThat(document.versions()).extracting(LegalVersionDto::state)
			.containsExactly(VersionState.UPCOMING, VersionState.CURRENT, VersionState.PAST);
	}

	@Test
	@DisplayName("US3-AC4 시행일 전날은 예정이고, 시행일 당일(한국 날짜)부터 지금 버전이다")
	void switchesOnEffectiveDate() {
		LegalDocumentDto dayBefore = service.document("terms", LocalDate.of(2098, 12, 31));
		LegalDocumentDto onDay = service.document("terms", LocalDate.of(2099, 1, 1));

		assertThat(dayBefore.current().version().version()).isEqualTo("draft");
		assertThat(service.version("terms", "next", LocalDate.of(2098, 12, 31)).state())
			.isEqualTo(VersionState.UPCOMING);
		assertThat(onDay.current().version().version()).isEqualTo("next");
		assertThat(onDay.upcoming()).isNull();
		assertThat(service.version("terms", "draft", LocalDate.of(2099, 1, 1)).state()).isEqualTo(VersionState.PAST);
		assertThat(service.version("terms", "draft", LocalDate.of(2099, 1, 1)).currentVersion()).isEqualTo("next");
	}

	@Test
	@DisplayName("없는 문서나 버전은 R001이다")
	void notFound() {
		LocalDate today = LocalDate.of(2026, 11, 28);

		assertThatThrownBy(() -> service.document("cookies", today)).isInstanceOf(ServiceException.class)
			.extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
		assertThatThrownBy(() -> service.version("privacy", "none", today)).isInstanceOf(ServiceException.class)
			.extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
	}
}
