package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.moa.common.security.Role;
import com.moa.support.AcceptanceTest;
import com.moa.support.TestUsers;

/**
 * 접속 이력 (선택 헬퍼 login-history)
 */
class LoginHistoryAcceptanceTest extends AcceptanceTest {

	private static final String CHROME_WINDOWS = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
		+ "(KHTML, like Gecko) Chrome/130.0 Safari/537.36";
	private static final String HISTORIES = "/api/v1/admin/login-histories";

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("접속 이력: 로그인 성공·실패를 기기 정보와 함께 남기고, 관리자가 최신순으로 본다")
	void recordAndList() {
		testUsers.create(10L, "kim", "secret123!", Role.USER);
		login("kim", "secret123!");
		login("kim", "wrong-password");

		var result = mvc.get().uri(HISTORIES).header(HttpHeaders.AUTHORIZATION, bearer(2L, "ADMIN")).exchange();

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$.totalElements").isEqualTo(2);
		assertThat(result).bodyJson().extractingPath("$.content[0].success").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.content[0].failureCode").isEqualTo("A006");
		assertThat(result).bodyJson().extractingPath("$.content[1].userId").isEqualTo(10);
		assertThat(result).bodyJson().extractingPath("$.content[1].os").isEqualTo("Windows");
		assertThat(result).bodyJson().extractingPath("$.content[1].browser").isEqualTo("Chrome");
	}

	@Test
	@DisplayName("접속 이력: 관리자가 아니면 A005")
	void adminOnly() {
		assertThat(mvc.get().uri(HISTORIES).header(HttpHeaders.AUTHORIZATION, bearer(3L, "USER")))
			.hasStatus(403)
			.bodyJson().extractingPath("$.code").isEqualTo("A005");
	}

	@Test
	@DisplayName("접속 이력: 아이디(정확히 일치)와 결과(성공·실패)로 거르고, 함께 주면 AND다")
	void filterByLoginIdAndSuccess() {
		history(10L, "kim", true, "2026-11-01T09:00:00");
		history(null, "kim", false, "2026-11-01T10:00:00");
		history(null, "kimchi", false, "2026-11-01T11:00:00");
		history(11L, "lee", true, "2026-11-01T12:00:00");

		assertThat(search("loginId=kim")).bodyJson().extractingPath("$.totalElements").isEqualTo(2);
		assertThat(search("success=false")).bodyJson().extractingPath("$.totalElements").isEqualTo(2);
		MvcTestResult kimFailed = search("loginId=kim&success=false");
		assertThat(kimFailed).bodyJson().extractingPath("$.totalElements").isEqualTo(1);
		assertThat(kimFailed).bodyJson().extractingPath("$.content[0].loginId").isEqualTo("kim");
		assertThat(search("userId=10")).bodyJson().extractingPath("$.totalElements").isEqualTo(1);
	}

	@Test
	@DisplayName("접속 이력: 기간은 양 끝 날짜를 포함하고(종료일은 그날 끝까지), 최신순이다")
	void filterByPeriod() {
		history(null, "kim", false, "2026-10-31T23:59:59");
		history(null, "kim", false, "2026-11-01T00:00:00");
		history(null, "kim", false, "2026-11-15T12:00:00");
		history(null, "kim", false, "2026-11-30T23:59:59");
		history(null, "kim", false, "2026-12-01T00:00:00");

		MvcTestResult november = search("from=2026-11-01&to=2026-11-30");

		assertThat(november).bodyJson().extractingPath("$.totalElements").isEqualTo(3);
		assertThat(november).bodyJson().extractingPath("$.content[0].createdAt").asString().startsWith("2026-11-30");
		// 시각은 시간대(오프셋)를 붙여 내보낸다 (CONTRACT.md 4절)
		assertThat(november).bodyJson().extractingPath("$.content[0].createdAt").asString()
			.matches(".*([+-]\\d{2}:\\d{2}|Z)$");
		assertThat(november).bodyJson().extractingPath("$.content[2].createdAt").asString().startsWith("2026-11-01");
		assertThat(search("from=2026-11-16")).bodyJson().extractingPath("$.totalElements").isEqualTo(2);
		assertThat(search("to=2026-10-31")).bodyJson().extractingPath("$.totalElements").isEqualTo(1);
	}

	@Test
	@DisplayName("접속 이력: 시작일이 종료일보다 늦으면 V001")
	void invalidPeriod() {
		assertThat(search("from=2026-11-30&to=2026-11-01"))
			.hasStatus(400)
			.bodyJson().extractingPath("$.code").isEqualTo("V001");
	}

	private MvcTestResult search(String query) {
		return mvc.get().uri(HISTORIES + "?" + query).header(HttpHeaders.AUTHORIZATION, bearer(2L, "ADMIN")).exchange();
	}

	/** 시각을 정해 이력을 넣는다 (로그인으로 남기면 시각이 지금이라 기간을 시험할 수 없다) */
	private void history(Long userId, String loginId, boolean success, String createdAt) {
		jdbcTemplate.update("insert into login_histories (user_id, login_id, success, failure_code, device_type, os, "
			+ "browser, created_at, updated_at) values (?, ?, ?, ?, 'PC', 'Windows', 'Chrome', ?, ?)",
			userId, loginId, success, success ? null : "A006", createdAt, createdAt);
	}

	private void login(String loginId, String password) {
		mvc.post().uri("/api/v1/auth/login")
			.header(HttpHeaders.USER_AGENT, CHROME_WINDOWS)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"loginId\":\"" + loginId + "\",\"password\":\"" + password + "\"}")
			.exchange();
	}
}
