package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;
import com.moa.api.application.v1.user.UserApplication;
import com.moa.common.audit.SystemActor;
import com.moa.common.security.Role;
import com.moa.support.AcceptanceTest;
import com.moa.support.DatabaseCleaner;
import com.moa.support.TestUsers;

/**
 * 관리자·운영자 계정. 스펙 specs/002-admin-users의 수용 시나리오다
 */
class AdminUsersAcceptanceTest extends AcceptanceTest {

	private static final String LOGIN = "/api/v1/auth/login";
	private static final String ME = "/api/v1/users/me";
	private static final String MY_PASSWORD = "/api/v1/users/me/password";
	private static final String ADMIN_USERS = "/api/v1/admin/users";

	@Autowired
	private UserApplication userApplication;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private DatabaseCleaner databaseCleaner;

	/** 로컬 계정 SQL의 admin이 남아 있을 수 있어 비우고 시작한다 */
	@BeforeEach
	void clean() {
		databaseCleaner.clean();
	}

	@Test
	@DisplayName("US1-AC1 첫 관리자는 임시 비밀번호로 로그인해 비밀번호를 바꾼 뒤 백오피스에 들어간다")
	void firstAdminChangesPasswordThenEnters() {
		SystemActor.call(() -> userApplication.bootstrapAdmin("root", "temporary123!", null));

		MvcTestResult firstLogin = login("root", "temporary123!");
		assertThat(firstLogin).hasStatus(200);
		String temporaryToken = accessTokenOf(firstLogin);
		assertThat(me(temporaryToken)).bodyJson().extractingPath("$.passwordChangeRequired").isEqualTo(true);

		assertThat(mvc.put().uri(MY_PASSWORD).header(HttpHeaders.AUTHORIZATION, temporaryToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"currentPassword\":\"temporary123!\",\"newPassword\":\"changed456!\"}"))
			.hasStatus(204);

		MvcTestResult secondLogin = login("root", "changed456!");
		assertThat(secondLogin).hasStatus(200);
		String token = accessTokenOf(secondLogin);
		assertThat(me(token)).bodyJson().extractingPath("$.passwordChangeRequired").isEqualTo(false);
		assertThat(mvc.get().uri(ADMIN_USERS).header(HttpHeaders.AUTHORIZATION, token)).hasStatus(200);
	}

	@Test
	@DisplayName("US2-AC1 관리자가 운영자 계정을 만들면 임시 비밀번호가 한 번 보이고 첫 로그인 때 변경을 요구한다")
	void adminCreatesOperatorWithTemporaryPassword() {
		testUsers.create(1L, "system", null, Role.USER);
		testUsers.create(2L, "admin", "admin1234!", Role.ADMIN);
		String admin = bearer(2L, "ADMIN");

		MvcTestResult created = mvc.post().uri(ADMIN_USERS).header(HttpHeaders.AUTHORIZATION, admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"loginId\":\"operator\",\"name\":\"운영자\",\"role\":\"USER\"}")
			.exchange();
		assertThat(created).hasStatus(201);
		assertThat(created).bodyJson().hasPath("$.temporaryPassword");
		String temporaryPassword = JsonPath.read(body(created), "$.temporaryPassword");
		Number operatorId = JsonPath.read(body(created), "$.user.id");

		MvcTestResult fetched = mvc.get().uri(ADMIN_USERS + "/" + operatorId)
			.header(HttpHeaders.AUTHORIZATION, admin)
			.exchange();
		assertThat(fetched).hasStatus(200);
		assertThat(fetched).bodyJson().doesNotHavePath("$.temporaryPassword");

		MvcTestResult operatorLogin = login("operator", temporaryPassword);
		assertThat(operatorLogin).hasStatus(200);
		assertThat(me(accessTokenOf(operatorLogin))).bodyJson().extractingPath("$.passwordChangeRequired")
			.isEqualTo(true);
	}

	private MvcTestResult login(String loginId, String password) {
		return mvc.post().uri(LOGIN).contentType(MediaType.APPLICATION_JSON)
			.content("{\"loginId\":\"" + loginId + "\",\"password\":\"" + password + "\"}")
			.exchange();
	}

	private MvcTestResult me(String bearer) {
		return mvc.get().uri(ME).header(HttpHeaders.AUTHORIZATION, bearer).exchange();
	}

	private String accessTokenOf(MvcTestResult login) {
		return "Bearer " + JsonPath.read(body(login), "$.accessToken");
	}

	private String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString();
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
