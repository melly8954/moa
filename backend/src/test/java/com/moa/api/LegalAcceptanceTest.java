package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.moa.common.security.Role;
import com.moa.support.AcceptanceTest;
import com.moa.support.TestUsers;

/**
 * 이용약관·개인정보처리방침 공개 API. 스펙 specs/012-terms-privacy의 수용 시나리오다 (contracts/api.md "AC와 확인 방법").
 * 테스트용 문안(classpath:legal-test/)은 문서마다 old(2000-01-01), draft(2020-01-01), next(2099-01-01) 세 버전이다.
 * 날짜 전환(US3-AC4)은 단위 테스트 LegalServiceTest가, 화면에서만 보이는 AC는 e2e(legal)가 본다 (research R8).
 */
@TestPropertySource(properties = "app.legal.location=classpath:legal-test/")
class LegalAcceptanceTest extends AcceptanceTest {

	private static final String LEGAL = "/api/v1/legal";
	private static final long MEMBER = 31L;

	@Autowired
	private TestUsers testUsers;

	@Test
	@DisplayName("US1-AC1 로그인하지 않아도 지금 시행 중인 개인정보처리방침의 제목·시행일·장을 받는다")
	void anyoneReadsPrivacy() {
		MvcTestResult result = mvc.get().uri(LEGAL + "/privacy").exchange();

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$.document").isEqualTo("privacy");
		assertThat(result).bodyJson().extractingPath("$.current.version").isEqualTo("draft");
		assertThat(result).bodyJson().extractingPath("$.current.title").isEqualTo("privacy draft");
		assertThat(result).bodyJson().extractingPath("$.current.effectiveDate").isEqualTo("2020-01-01");
		assertThat(result).bodyJson().extractingPath("$.current.sections[*].id").asArray()
			.containsExactly("first", "retention");
		assertThat(result).bodyJson().extractingPath("$.current.sections[1].heading").isEqualTo("보유 기간");
		assertThat(result).bodyJson().extractingPath("$.current.sections[1].html").asString().contains("<table>")
			.doesNotContain("<h2>");
	}

	@Test
	@DisplayName("US1-AC2 로그인하지 않아도 지금 시행 중인 이용약관을 받는다")
	void anyoneReadsTerms() {
		MvcTestResult result = mvc.get().uri(LEGAL + "/terms").exchange();

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$.document").isEqualTo("terms");
		assertThat(result).bodyJson().extractingPath("$.current.version").isEqualTo("draft");
		assertThat(result).bodyJson().extractingPath("$.current.title").isEqualTo("terms draft");
		assertThat(result).bodyJson().extractingPath("$.current.sections[*].heading").asArray()
			.containsExactly("첫 장 (draft)", "둘째 장");
	}

	@Test
	@DisplayName("US1-AC3 회원과 관리자 계정도 로그인하지 않았을 때와 같은 문서를 받는다")
	void sameForMemberAndAdmin() {
		testUsers.create(MEMBER, "member31", "member1234!", Role.USER);
		String anonymous = body(mvc.get().uri(LEGAL + "/privacy").exchange());

		MvcTestResult asMember = mvc.get().uri(LEGAL + "/privacy")
			.header(HttpHeaders.AUTHORIZATION, bearer(MEMBER, Role.USER.name())).exchange();
		MvcTestResult asAdmin = mvc.get().uri(LEGAL + "/privacy")
			.header(HttpHeaders.AUTHORIZATION, superAdmin()).exchange();

		assertThat(asMember).hasStatus(200);
		assertThat(asAdmin).hasStatus(200);
		assertThat(body(asMember)).isEqualTo(anonymous);
		assertThat(body(asAdmin)).isEqualTo(anonymous);
	}

	@Test
	@DisplayName("US3-AC1 버전 목록은 시행일 최근순이고 예정·지금·예전 상태가 붙는다")
	void versionsLatestFirst() {
		MvcTestResult result = mvc.get().uri(LEGAL + "/privacy").exchange();

		assertThat(result).bodyJson().extractingPath("$.versions[*].version").asArray()
			.containsExactly("next", "draft", "old");
		assertThat(result).bodyJson().extractingPath("$.versions[*].state").asArray()
			.containsExactly("UPCOMING", "CURRENT", "PAST");
		assertThat(result).bodyJson().extractingPath("$.versions[0].effectiveDate").isEqualTo("2099-01-01");
	}

	@Test
	@DisplayName("US3-AC2 예전 버전을 열면 그 전문과 예전 상태, 지금 버전 이름을 받는다")
	void pastVersion() {
		MvcTestResult result = mvc.get().uri(LEGAL + "/privacy/versions/old").exchange();

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$.version").isEqualTo("old");
		assertThat(result).bodyJson().extractingPath("$.effectiveDate").isEqualTo("2000-01-01");
		assertThat(result).bodyJson().extractingPath("$.state").isEqualTo("PAST");
		assertThat(result).bodyJson().extractingPath("$.currentVersion").isEqualTo("draft");
		assertThat(result).bodyJson().extractingPath("$.sections[0].html").asString().contains("privacy old 본문");
	}

	@Test
	@DisplayName("US3-AC3 시행 예정 버전이 있으면 지금 문서에 예정 안내가 오고, 예정 버전을 미리 본다")
	void upcomingVersion() {
		MvcTestResult document = mvc.get().uri(LEGAL + "/privacy").exchange();
		MvcTestResult upcoming = mvc.get().uri(LEGAL + "/privacy/versions/next").exchange();

		assertThat(document).bodyJson().extractingPath("$.upcoming.version").isEqualTo("next");
		assertThat(document).bodyJson().extractingPath("$.upcoming.effectiveDate").isEqualTo("2099-01-01");
		assertThat(upcoming).hasStatus(200);
		assertThat(upcoming).bodyJson().extractingPath("$.state").isEqualTo("UPCOMING");
		assertThat(upcoming).bodyJson().extractingPath("$.currentVersion").isEqualTo("draft");
	}

	@Test
	@DisplayName("없는 문서나 없는 버전은 404 R001이다")
	void notFound() {
		MvcTestResult document = mvc.get().uri(LEGAL + "/cookies").exchange();
		MvcTestResult version = mvc.get().uri(LEGAL + "/privacy/versions/none").exchange();

		assertThat(document).hasStatus(404);
		assertThat(document).bodyJson().extractingPath("$.code").isEqualTo("R001");
		assertThat(version).hasStatus(404);
		assertThat(version).bodyJson().extractingPath("$.code").isEqualTo("R001");
	}

	private static String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}
}
