package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
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
 * 계정 상태: 잠금, 비밀번호 변경 요구, 내 비밀번호 바꾸기.
 */
class UserAccountAcceptanceTest extends AcceptanceTest {

	private static final String LOGIN = "/api/v1/auth/login";
	private static final String ME = "/api/v1/users/me";
	private static final String PASSWORD = "/api/v1/users/me/password";

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void createUser() {
		testUsers.create(10L, "kim", "secret123!", Role.USER);
	}

	@Test
	@DisplayName("계정: 잠긴 계정은 비밀번호가 맞으면 A007, 틀리면 그대로 A006")
	void lockedLogin() {
		jdbcTemplate.update("update users set locked_at = now(6) where id = 10");

		assertThat(login("kim", "secret123!")).hasStatus(401).bodyJson().extractingPath("$.code").isEqualTo("A007");
		assertThat(login("kim", "wrong-password")).hasStatus(401).bodyJson().extractingPath("$.code")
			.isEqualTo("A006");
	}

	@Test
	@DisplayName("계정: 임시 비밀번호를 받으면 내 정보에 passwordChangeRequired가 보이고, 바꾸면 풀린다")
	void changeRequired() {
		jdbcTemplate.update("update users set password_change_required = true where id = 10");
		String user = bearer(10L, "USER");

		assertThat(mvc.get().uri(ME).header(HttpHeaders.AUTHORIZATION, user)).bodyJson()
			.extractingPath("$.passwordChangeRequired").isEqualTo(true);
		assertThat(changePassword(user, "secret123!", "newSecret456!")).hasStatus(204);
		assertThat(mvc.get().uri(ME).header(HttpHeaders.AUTHORIZATION, user)).bodyJson()
			.extractingPath("$.passwordChangeRequired").isEqualTo(false);
		assertThat(login("kim", "newSecret456!")).hasStatus(200);
		assertThat(login("kim", "secret123!")).hasStatus(401);
	}

	@Test
	@DisplayName("계정: 현재 비밀번호가 틀리거나, 새 비밀번호가 같거나 짧으면 V001, 비밀번호 없는 계정은 B001")
	void changeErrors() {
		String user = bearer(10L, "USER");
		assertThat(changePassword(user, "wrong", "newSecret456!")).hasStatus(400)
			.bodyJson().extractingPath("$.code").isEqualTo("V001");
		assertThat(changePassword(user, "secret123!", "secret123!")).hasStatus(400);
		assertThat(changePassword(user, "secret123!", "short")).hasStatus(400);

		testUsers.create(11L, "google_1", null, Role.USER);
		assertThat(changePassword(bearer(11L, "USER"), "x", "newSecret456!")).hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("B001");
	}

	private MvcTestResult login(String loginId, String password) {
		return mvc.post().uri(LOGIN).contentType(MediaType.APPLICATION_JSON)
			.content("{\"loginId\":\"" + loginId + "\",\"password\":\"" + password + "\"}")
			.exchange();
	}

	private MvcTestResult changePassword(String bearer, String current, String next) {
		return mvc.put().uri(PASSWORD).header(HttpHeaders.AUTHORIZATION, bearer)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"currentPassword\":\"" + current + "\",\"newPassword\":\"" + next + "\"}")
			.exchange();
	}
}
