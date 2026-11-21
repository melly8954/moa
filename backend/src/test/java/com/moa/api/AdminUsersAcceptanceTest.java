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
		// moa (2026-11-21): 첫 관리자는 최고 관리자 역할을 가져 모든 관리 권한을 갖는다
		assertThat(mvc.get().uri("/api/v1/users/me/permissions").header(HttpHeaders.AUTHORIZATION, token))
			.bodyJson().extractingPath("$.permissions").asArray().contains("USER_MANAGE", "ROLE_MANAGE");
	}

	@Test
	@DisplayName("US2-AC1 관리자가 계정 종류 '관리자'로 운영자 계정을 만들면 임시 비밀번호가 한 번 보이고, 첫 로그인 때 변경을 요구하며, 역할 전에는 관리 메뉴가 없다")
	void adminCreatesOperatorWithTemporaryPassword() {
		testUsers.create(1L, "system", null, Role.USER);
		testUsers.create(2L, "admin", "admin1234!", Role.ADMIN);
		String admin = superAdmin();

		MvcTestResult created = mvc.post().uri(ADMIN_USERS).header(HttpHeaders.AUTHORIZATION, admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"loginId\":\"operator\",\"name\":\"운영자\",\"role\":\"ADMIN\"}")
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
		assertThat(mvc.get().uri("/api/v1/menus/me").header(HttpHeaders.AUTHORIZATION, accessTokenOf(operatorLogin)))
			.hasStatus(200).bodyJson().extractingPath("$.length()").isEqualTo(0);
	}

	@Test
	@DisplayName("US3-AC1 사용자 관리 목록에 역할이 보인다 (운영자는 그 역할, 최고 관리자는 superAdmin, 역할 없는 관리자 계정과 회원은 빈 목록)")
	void listShowsAccessRoles() {
		testUsers.create(1L, "system", null, Role.USER);
		testUsers.create(2L, "admin", "admin1234!", Role.ADMIN);
		testUsers.create(10L, "operator", "operator123!", Role.ADMIN);
		testUsers.create(12L, "norole", "norole1234!", Role.ADMIN);
		testUsers.create(11L, "member", "member1234!", Role.USER);
		String admin = superAdmin();
		MvcTestResult role = mvc.post().uri("/api/v1/admin/roles").header(HttpHeaders.AUTHORIZATION, admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"SECURITY\",\"name\":\"보안 담당\",\"permissions\":[\"LOGIN_HISTORY_READ\"]}")
			.exchange();
		Number roleId = JsonPath.read(body(role), "$.id");
		assertThat(mvc.post().uri("/api/v1/admin/roles/" + roleId + "/members").header(HttpHeaders.AUTHORIZATION, admin)
			.contentType(MediaType.APPLICATION_JSON).content("{\"userIds\":[10]}").exchange())
			.hasStatus(204);

		MvcTestResult result = mvc.get().uri(ADMIN_USERS).header(HttpHeaders.AUTHORIZATION, admin).exchange();

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$.content[?(@.loginId == 'operator')].accessRoles[*].name")
			.asArray().containsExactly("보안 담당");
		assertThat(result).bodyJson().extractingPath("$.content[?(@.loginId == 'member')].accessRoles[*]")
			.asArray().isEmpty();
		assertThat(result).bodyJson().extractingPath("$.content[?(@.loginId == 'admin')].superAdmin")
			.asArray().containsExactly(true);
		assertThat(result).bodyJson().extractingPath("$.content[?(@.loginId == 'norole')].accessRoles[*]")
			.asArray().isEmpty();
		assertThat(result).bodyJson().extractingPath("$.content[?(@.loginId == 'operator')].superAdmin")
			.asArray().containsExactly(false);
		assertThat(mvc.get().uri(ADMIN_USERS + "/10").header(HttpHeaders.AUTHORIZATION, admin).exchange())
			.bodyJson().extractingPath("$.accessRoles[0].name").isEqualTo("보안 담당");
	}

	@Test
	@DisplayName("US3-AC3 드로어가 쓰는 역할 저장(PUT /users/{id}/roles)으로 역할을 더하면 그 운영자는 다음 요청부터 그 메뉴를 쓴다")
	void drawerGivesRoles() {
		String admin = superAdmin();
		testUsers.create(10L, "operator", "operator123!", Role.ADMIN);
		Number security = createRole(admin, "SECURITY", "LOGIN_HISTORY_READ");
		Number notices = createRole(admin, "NOTICES", "NOTICE_MANAGE");

		assertThat(mvc.put().uri("/api/v1/admin/users/10/roles").header(HttpHeaders.AUTHORIZATION, admin)
			.contentType(MediaType.APPLICATION_JSON).content("{\"roleIds\":[" + security + "," + notices + "]}")
			.exchange()).hasStatus(204);
		assertThat(mvc.get().uri("/api/v1/admin/notices").header(HttpHeaders.AUTHORIZATION, bearer(10L, "ADMIN"))
			.exchange()).hasStatus(200);
	}

	@Test
	@DisplayName("US4-AC1 사용자 관리 운영자는 회원을 다루고 계정 종류를 관리자로 바꾼다")
	void managerHandlesMembers() {
		superAdmin();
		String manager = operator(20L, "manager", "USER_MANAGE");
		testUsers.create(11L, "member", "member1234!", Role.USER);

		assertThat(mvc.put().uri(ADMIN_USERS + "/11").header(HttpHeaders.AUTHORIZATION, manager)
			.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"회원\"}").exchange()).hasStatus(200);
		assertThat(mvc.post().uri(ADMIN_USERS + "/11/password-reset").header(HttpHeaders.AUTHORIZATION, manager)
			.exchange()).hasStatus(200);
		assertThat(mvc.put().uri(ADMIN_USERS + "/11/role").header(HttpHeaders.AUTHORIZATION, manager)
			.contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}").exchange()).hasStatus(200)
			.bodyJson().extractingPath("$.role").isEqualTo("ADMIN");
	}

	@Test
	@DisplayName("US4-AC2·AC3 사용자 관리 운영자는 권한이 더 많은 운영자와 최고 관리자를 다루지 못한다(A005, protection)")
	void managerCannotTouchWiderAccounts() {
		superAdmin();
		String manager = operator(20L, "manager", "USER_MANAGE");
		operator(21L, "security", "LOGIN_HISTORY_READ");

		assertThat(mvc.post().uri(ADMIN_USERS + "/21/password-reset").header(HttpHeaders.AUTHORIZATION, manager)
			.exchange()).hasStatus(403).bodyJson().extractingPath("$.code").isEqualTo("A005");
		assertThat(mvc.put().uri(ADMIN_USERS + "/2/lock").header(HttpHeaders.AUTHORIZATION, manager).exchange())
			.hasStatus(403).bodyJson().extractingPath("$.code").isEqualTo("A005");

		MvcTestResult list = mvc.get().uri(ADMIN_USERS).header(HttpHeaders.AUTHORIZATION, manager).exchange();
		assertThat(list).bodyJson().extractingPath("$.content[?(@.loginId == 'security')].protection").asArray()
			.containsExactly("WIDER_PERMISSIONS");
		assertThat(list).bodyJson().extractingPath("$.content[?(@.loginId == 'admin')].protection").asArray()
			.containsExactly("SUPER_ADMIN_ONLY");
	}

	@Test
	@DisplayName("US4-AC5 관리자 계정을 회원으로 바꾸면 역할이 모두 빠지고 로그아웃된다")
	void demoteRemovesRoles() {
		String admin = superAdmin();
		operator(21L, "security", "LOGIN_HISTORY_READ");
		MvcTestResult login = login("security", "operator1234!");
		assertThat(login).hasStatus(200);

		assertThat(mvc.put().uri(ADMIN_USERS + "/21/role").header(HttpHeaders.AUTHORIZATION, admin)
			.contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"USER\"}").exchange()).hasStatus(200);
		assertThat(mvc.get().uri("/api/v1/admin/users/21/roles").header(HttpHeaders.AUTHORIZATION, admin).exchange())
			.bodyJson().extractingPath("$.length()").isEqualTo(0);
		assertThat(mvc.post().uri("/api/v1/auth/token/refresh").cookie(login.getResponse().getCookies()).exchange())
			.hasStatus(401);
	}

	@Test
	@DisplayName("FR-007 하나뿐인 최고 관리자는 다른 최고 관리자 없이 잠그거나 지우거나 회원으로 바꿀 수 없다(B001)")
	void lastSuperAdminIsProtected() {
		String admin = superAdmin();
		testUsers.create(3L, "second", "second1234!", Role.ADMIN);
		testUsers.makeSuperAdmin(3L);
		String second = bearer(3L, "ADMIN");

		assertThat(mvc.put().uri(ADMIN_USERS + "/3/role").header(HttpHeaders.AUTHORIZATION, admin)
			.contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"USER\"}").exchange()).hasStatus(200);
		assertThat(mvc.put().uri(ADMIN_USERS + "/2/lock").header(HttpHeaders.AUTHORIZATION, second).exchange())
			.hasStatus(403);
		assertThat(mvc.delete().uri(ADMIN_USERS + "/2").header(HttpHeaders.AUTHORIZATION, admin).exchange())
			.hasStatus(409).bodyJson().extractingPath("$.code").isEqualTo("B001");
	}

	private Number createRole(String admin, String code, String permission) {
		MvcTestResult role = mvc.post().uri("/api/v1/admin/roles").header(HttpHeaders.AUTHORIZATION, admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"" + code + "\",\"name\":\"" + code + "\",\"permissions\":[\"" + permission + "\"]}")
			.exchange();
		assertThat(role).hasStatus(201);
		return JsonPath.read(body(role), "$.id");
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
