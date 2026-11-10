package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.moa.common.helper.loginhistory.LoginHistoryCleaner;
import com.moa.common.security.Role;
import com.moa.support.AcceptanceTest;
import com.moa.support.TestUsers;

/**
 * 접속 이력. 스펙 specs/005-admin-login-history의 수용 시나리오와 보관 기간(FR-002)이다
 */
class AdminLoginHistoryAcceptanceTest extends AcceptanceTest {

	private static final String CHROME_WINDOWS = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
		+ "(KHTML, like Gecko) Chrome/130.0 Safari/537.36";
	/** 문서용 주소 (RFC 5737) */
	private static final String CLIENT_IP = "203.0.113.10";

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private LoginHistoryCleaner loginHistoryCleaner;

	@Test
	@DisplayName("US1-AC1 관리자가 아이디로 찾으면 그 아이디의 실패와 성공이 최신순으로 접속 IP, 기기와 함께 보인다")
	void adminFindsHistoriesByLoginId() {
		testUsers.create(10L, "kim", "secret123!", Role.USER);
		testUsers.create(11L, "lee", "secret456!", Role.USER);
		login("kim", "wrong-password");
		login("kim", "secret123!");
		login("lee", "secret456!");

		MvcTestResult result = mvc.get().uri("/api/v1/admin/login-histories?loginId=kim")
			.header(HttpHeaders.AUTHORIZATION, bearer(2L, "ADMIN"))
			.exchange();

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$.totalElements").isEqualTo(2);
		assertThat(result).bodyJson().extractingPath("$.content[0].success").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.content[1].success").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.content[1].failureCode").isEqualTo("A006");
		assertThat(result).bodyJson().extractingPath("$.content[*].ip").asArray()
			.containsExactly(CLIENT_IP, CLIENT_IP);
		assertThat(result).bodyJson().extractingPath("$.content[*].deviceType").asArray()
			.containsExactly("PC", "PC");
		assertThat(result).bodyJson().extractingPath("$.content[*].os").asArray()
			.containsExactly("Windows", "Windows");
		assertThat(result).bodyJson().extractingPath("$.content[*].browser").asArray()
			.containsExactly("Chrome", "Chrome");
	}

	@Test
	@DisplayName("FR-002 접속 이력은 90일 동안 보관하고, 지나면 정리 때 지운다")
	void keepsHistoriesFor90Days() {
		history("within-90-days", LocalDateTime.now().minusDays(89));
		history("over-90-days", LocalDateTime.now().minusDays(91));

		loginHistoryCleaner.deleteExpired();

		List<String> remaining = jdbcTemplate.queryForList("select login_id from login_histories", String.class);
		assertThat(remaining).containsExactly("within-90-days");
	}

	private void login(String loginId, String password) {
		mvc.post().uri("/api/v1/auth/login")
			.with(request -> {
				request.setRemoteAddr(CLIENT_IP);
				return request;
			})
			.header(HttpHeaders.USER_AGENT, CHROME_WINDOWS)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"loginId\":\"" + loginId + "\",\"password\":\"" + password + "\"}")
			.exchange();
	}

	private void history(String loginId, LocalDateTime createdAt) {
		jdbcTemplate.update("insert into login_histories (user_id, login_id, success, failure_code, device_type, os, "
			+ "browser, created_at, updated_at) values (null, ?, false, 'A006', 'PC', 'Windows', 'Chrome', ?, ?)",
			loginId, createdAt, createdAt);
	}
}
