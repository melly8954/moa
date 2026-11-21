package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
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
	private DataSource dataSource;

	@Autowired
	private PasswordEncoder passwordEncoder;

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
	@DisplayName("US2-AC1 관리자가 계정 종류 '관리자'로 운영자 계정을 만들면 임시 비밀번호가 한 번 보이고, 첫 로그인 때 변경을 요구하며, 역할 전에는 관리 메뉴가 모두 잠겨 있다")
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
			.hasStatus(200).bodyJson().extractingPath("$..[?(@.accessible == true)].name").asArray().isEmpty();
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
	// 2026-11-21 한 명 고정: 예전 "하나뿐인 최고 관리자는 다른 최고 관리자 없이 바꿀 수 없다"를 "최고 관리자 계정은 이름 말고 누구도 못
	// 바꾼다"로 바꿨다 (원칙 V, research R15)
	@DisplayName("US4-AC3·FR-007 최고 관리자 계정은 본인 이름 바꾸기만 되고, 계정 종류·잠그기·풀기·비밀번호 초기화·삭제는 본인 B001, 운영자 A005")
	void superAdminAccountIsFixed() {
		String admin = superAdmin();
		String manager = operator(20L, "manager", "USER_MANAGE");

		assertThat(mvc.put().uri(ADMIN_USERS + "/2").header(HttpHeaders.AUTHORIZATION, admin)
			.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"대표\"}").exchange()).hasStatus(200);
		assertThat(mvc.put().uri(ADMIN_USERS + "/2/role").header(HttpHeaders.AUTHORIZATION, admin)
			.contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"USER\"}").exchange()).hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("B001");
		assertThat(
			mvc.post().uri(ADMIN_USERS + "/2/password-reset").header(HttpHeaders.AUTHORIZATION, admin).exchange())
			.hasStatus(409).bodyJson().extractingPath("$.code").isEqualTo("B001");
		assertThat(mvc.delete().uri(ADMIN_USERS + "/2/lock").header(HttpHeaders.AUTHORIZATION, admin).exchange())
			.hasStatus(409);
		assertThat(mvc.put().uri(ADMIN_USERS + "/2").header(HttpHeaders.AUTHORIZATION, manager)
			.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"남\"}").exchange()).hasStatus(403)
			.bodyJson().extractingPath("$.code").isEqualTo("A005");
		assertThat(mvc.delete().uri(ADMIN_USERS + "/2").header(HttpHeaders.AUTHORIZATION, manager).exchange())
			.hasStatus(403);
	}

	@Test
	@DisplayName("US1-AC2 개발자가 비밀번호 되돌리기 SQL을 실행하면 최고 관리자는 임시 비밀번호로 로그인해 변경 요구를 받고, 예전 로그인은 끊긴다")
	void developerResetsSuperAdminPassword() {
		superAdmin();
		MvcTestResult before = login("admin", "admin1234!");
		assertThat(before).hasStatus(200);

		runOps("super-admin-reset-password.sql", "SET @login := 'admin';",
			"SET @hash := '" + passwordEncoder.encode("Reset1234!") + "';");

		MvcTestResult after = login("admin", "Reset1234!");
		assertThat(after).hasStatus(200);
		assertThat(me(accessTokenOf(after))).bodyJson().extractingPath("$.passwordChangeRequired").isEqualTo(true);
		assertThat(login("admin", "admin1234!")).hasStatus(401);
		assertThat(mvc.post().uri("/api/v1/auth/token/refresh").cookie(before.getResponse().getCookies()).exchange())
			.hasStatus(401);
	}

	@Test
	@DisplayName("US1-AC3 개발자가 최고 관리자 바꾸기 SQL을 실행하면 그 관리자 계정이 최고 관리자가 되고 예전 최고 관리자는 권한이 없다")
	void developerTransfersSuperAdmin() {
		String oldAdmin = superAdmin();
		String newAdmin = operator(30L, "next", "LOGIN_HISTORY_READ");

		runOps("super-admin-transfer.sql", "SET @to := 'next';");

		assertThat(mvc.get().uri("/api/v1/users/me/permissions").header(HttpHeaders.AUTHORIZATION, newAdmin).exchange())
			.bodyJson().extractingPath("$.permissions").asArray().contains("USER_MANAGE", "ROLE_MANAGE");
		assertThat(mvc.get().uri("/api/v1/users/me/permissions").header(HttpHeaders.AUTHORIZATION, oldAdmin).exchange())
			.bodyJson().extractingPath("$.permissions").asArray().isEmpty();
		assertThat(mvc.get().uri("/api/v1/admin/users/30/roles").header(HttpHeaders.AUTHORIZATION, newAdmin).exchange())
			.bodyJson().extractingPath("$[*].code").asArray().containsExactly("SUPER_ADMIN");
	}

	@Test
	@DisplayName("FR-009 최고 관리자 바꾸기 SQL은 회원이나 없는 아이디면 멈추고 아무것도 바꾸지 않는다")
	void developerProcedureStopsOnWrongTarget() {
		String admin = superAdmin();
		testUsers.create(31L, "member", "member1234!", Role.USER);

		assertThatThrownBy(() -> runOps("super-admin-transfer.sql", "SET @to := 'member';"))
			.isInstanceOf(ScriptException.class)
			.hasStackTraceContaining("Column 'ok' cannot be null");
		assertThatThrownBy(() -> runOps("super-admin-transfer.sql", "SET @to := 'nobody';"))
			.isInstanceOf(ScriptException.class)
			.hasStackTraceContaining("Column 'ok' cannot be null");
		assertThat(mvc.get().uri("/api/v1/users/me/permissions").header(HttpHeaders.AUTHORIZATION, admin).exchange())
			.bodyJson().extractingPath("$.permissions").asArray().contains("USER_MANAGE");
	}

	/** 개발자 절차 SQL(db/ops)을 맨 위 값 줄만 바꿔 한 연결에서 실행한다 (research R14·R15) */
	private void runOps(String file, String... valueLines) {
		try {
			String sql = new ClassPathResource("db/ops/" + file).getContentAsString(StandardCharsets.UTF_8);
			for (String line : valueLines) {
				String prefix = line.substring(0, line.indexOf(":=") + 2);
				sql = sql.replaceFirst("(?m)^" + java.util.regex.Pattern.quote(prefix) + ".*$",
					java.util.regex.Matcher.quoteReplacement(line));
			}
			try (Connection connection = dataSource.getConnection()) {
				try {
					ScriptUtils.executeSqlScript(connection, new EncodedResource(
						new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
				} finally {
					// 절차가 중간에 멈추면 연결 풀에 열린 트랜잭션과 임시 표가 남지 않게 한다
					try (var statement = connection.createStatement()) {
						statement.execute("ROLLBACK");
						statement.execute("DROP TEMPORARY TABLE IF EXISTS moa_ops_check");
					}
				}
			}
		} catch (IOException | SQLException ex) {
			throw new IllegalStateException(ex);
		}
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
