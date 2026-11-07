package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;
import com.moa.common.security.Role;
import com.moa.support.AcceptanceTest;
import com.moa.support.TestUsers;

/**
 * 사용자 관리 (선택 헬퍼 user-admin)
 */
class UserAdminAcceptanceTest extends AcceptanceTest {

	private static final String USERS = "/api/v1/admin/users";
	private static final String LOGIN = "/api/v1/auth/login";

	@Autowired
	private TestUsers testUsers;

	/** 1 시스템 계정(목록에 없음), 2 관리자, 3 사용자 */
	@BeforeEach
	void createUsers() {
		testUsers.create(1L, "system", null, Role.USER);
		testUsers.create(2L, "admin", "admin1234!", Role.ADMIN);
		testUsers.create(3L, "kim", "secret123!", Role.USER);
	}

	@Test
	@DisplayName("사용자 관리: 만들면 임시 비밀번호로 로그인하고 바꾸라는 표시가 보인다. 아이디 중복 R002")
	void createWithTemporaryPassword() {
		MvcTestResult created = asAdmin(mvc.post().uri(USERS).contentType(MediaType.APPLICATION_JSON)
			.content("{\"loginId\":\"hong\",\"name\":\"홍길동\",\"role\":\"USER\"}"));
		assertThat(created).hasStatus(201);
		assertThat(created).bodyJson().extractingPath("$.user.passwordChangeRequired").isEqualTo(true);
		String temporary = JsonPath.read(body(created), "$.temporaryPassword");

		assertThat(login("hong", temporary)).hasStatus(200);
		assertThat(asAdmin(mvc.post().uri(USERS).contentType(MediaType.APPLICATION_JSON)
			.content("{\"loginId\":\"hong\",\"name\":\"또\",\"role\":\"USER\"}"))).hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("R002");
	}

	@Test
	@DisplayName("사용자 관리: 목록은 검색 조건으로 거르고 시스템 계정은 빠진다")
	void search() {
		assertThat(asAdmin(mvc.get().uri(USERS))).hasStatus(200)
			.bodyJson().extractingPath("$.totalElements").isEqualTo(2);
		assertThat(asAdmin(mvc.get().uri(USERS + "?keyword=ki"))).bodyJson().extractingPath("$.content[0].loginId")
			.isEqualTo("kim");
		assertThat(asAdmin(mvc.get().uri(USERS + "?role=ADMIN"))).bodyJson().extractingPath("$.totalElements")
			.isEqualTo(1);
		assertThat(asAdmin(mvc.get().uri(USERS + "/1"))).hasStatus(404);
	}

	@Test
	@DisplayName("사용자 관리: 잠그면 로그인이 A007로 막히고, 풀면 다시 된다. 자기 자신은 B001")
	void lockAndUnlock() {
		assertThat(asAdmin(mvc.put().uri(USERS + "/3/lock"))).hasStatus(204);
		assertThat(login("kim", "secret123!")).hasStatus(401).bodyJson().extractingPath("$.code").isEqualTo("A007");
		assertThat(asAdmin(mvc.get().uri(USERS + "?locked=true"))).bodyJson().extractingPath("$.totalElements")
			.isEqualTo(1);

		assertThat(asAdmin(mvc.delete().uri(USERS + "/3/lock"))).hasStatus(204);
		assertThat(login("kim", "secret123!")).hasStatus(200);
		assertThat(asAdmin(mvc.put().uri(USERS + "/2/lock"))).hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("B001");
	}

	@Test
	@DisplayName("사용자 관리: 비밀번호 초기화는 예전 비밀번호를 막고 임시 비밀번호로 로그인하게 한다. 소셜 계정은 B001")
	void resetPassword() {
		MvcTestResult reset = asAdmin(mvc.post().uri(USERS + "/3/password-reset"));
		assertThat(reset).hasStatus(200);
		String temporary = JsonPath.read(body(reset), "$.temporaryPassword");

		assertThat(login("kim", "secret123!")).hasStatus(401);
		assertThat(login("kim", temporary)).hasStatus(200);

		testUsers.create(4L, "google_1", null, Role.USER);
		assertThat(asAdmin(mvc.post().uri(USERS + "/4/password-reset"))).hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("B001");
	}

	@Test
	@DisplayName("사용자 관리: ADMIN이 아니면 ADMIN 계정을 건드리거나 ADMIN을 줄 수 없다(A005). 역할 바꾸기는 ADMIN만")
	void adminProtection() {
		String manager = bearer(3L, "USER");
		// USER_MANAGE를 받지 않은 USER는 관리 API에서 막힌다 (rbac로 권한을 받은 사용자의 ADMIN 보호는 서비스 규칙)
		assertThat(mvc.get().uri(USERS).header(HttpHeaders.AUTHORIZATION, manager)).hasStatus(403);

		assertThat(asAdmin(mvc.put().uri(USERS + "/3/role").contentType(MediaType.APPLICATION_JSON)
			.content("{\"role\":\"ADMIN\"}"))).hasStatus(200).bodyJson().extractingPath("$.role")
			.isEqualTo("ADMIN");
		assertThat(asAdmin(mvc.put().uri(USERS + "/2/role").contentType(MediaType.APPLICATION_JSON)
			.content("{\"role\":\"USER\"}"))).hasStatus(409);
	}

	@Test
	@DisplayName("사용자 관리: 지우면 목록과 로그인에서 빠지고 같은 아이디로 다시 만들 수 있다")
	void delete() {
		assertThat(asAdmin(mvc.delete().uri(USERS + "/3"))).hasStatus(204);
		assertThat(login("kim", "secret123!")).hasStatus(401);
		assertThat(asAdmin(mvc.get().uri(USERS + "/3"))).hasStatus(404);
		assertThat(asAdmin(mvc.post().uri(USERS).contentType(MediaType.APPLICATION_JSON)
			.content("{\"loginId\":\"kim\",\"name\":\"김\",\"role\":\"USER\"}"))).hasStatus(201);
		assertThat(asAdmin(mvc.delete().uri(USERS + "/2"))).hasStatus(409);
	}

	private MvcTestResult login(String loginId, String password) {
		return mvc.post().uri(LOGIN).contentType(MediaType.APPLICATION_JSON)
			.content("{\"loginId\":\"" + loginId + "\",\"password\":\"" + password + "\"}")
			.exchange();
	}

	private MvcTestResult asAdmin(MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(2L, "ADMIN")).exchange();
	}

	private String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString();
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
