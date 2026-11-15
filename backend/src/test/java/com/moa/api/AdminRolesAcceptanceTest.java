package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

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
import com.moa.support.MenuSeed;
import com.moa.support.TestUsers;

/**
 * 운영자 역할. 스펙 specs/003-admin-roles의 수용 시나리오다
 */
class AdminRolesAcceptanceTest extends AcceptanceTest {

	private static final long OPERATOR_ID = 10L;
	private static final long OPERATOR2_ID = 11L;
	private static final long OPERATOR3_ID = 12L;
	private static final String ROLES = "/api/v1/admin/roles";
	private static final String LOGIN_HISTORIES = "/api/v1/admin/login-histories";

	/** 준비에서 만든 "보안 담당" 역할 (접속 이력 조회) */
	private Number securityRoleId;

	@Autowired
	private MenuSeed menuSeed;

	@Autowired
	private TestUsers testUsers;

	/** 메뉴 처음 구성에서, 관리자가 접속 이력 조회 권한만 묶은 역할을 만들어 운영자에게 준 상태로 시작한다 */
	@BeforeEach
	void giveOperatorLoginHistoryRole() {
		menuSeed.restore();
		testUsers.create(1L, "system", null, Role.USER);
		testUsers.create(2L, "admin", "admin1234!", Role.ADMIN);
		testUsers.create(OPERATOR_ID, "operator", "operator123!", Role.USER);

		MvcTestResult role = asAdmin(mvc.post().uri("/api/v1/admin/roles").contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"SECURITY\",\"name\":\"보안 담당\",\"permissions\":[\"LOGIN_HISTORY_READ\"]}"));
		assertThat(role).hasStatus(201);
		securityRoleId = JsonPath.read(body(role), "$.id");

		giveRoles(OPERATOR_ID, securityRoleId);
	}

	@Test
	@DisplayName("US1-AC1 접속 이력 조회 역할만 받은 운영자는 관리 메뉴에 접속 이력만 본다")
	void operatorSeesOnlyLoginHistoryMenu() {
		MvcTestResult result = asOperator(mvc.get().uri("/api/v1/menus/me"));

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$[*].name").asArray().containsExactly("운영");
		assertThat(result).bodyJson().extractingPath("$[0].children[*].name").asArray()
			.containsExactly("접속 이력");
	}

	@Test
	@DisplayName("US1-AC2 같은 운영자는 접속 이력 목록을 연다")
	void operatorOpensLoginHistories() {
		assertThat(asOperator(mvc.get().uri("/api/v1/admin/login-histories"))).hasStatus(200);
	}

	@Test
	@DisplayName("US1-AC3 같은 운영자가 사용자 관리에 들어가면 권한 없음(A005)으로 막힌다")
	void operatorIsForbiddenFromUsers() {
		assertThat(asOperator(mvc.get().uri("/api/v1/admin/users"))).hasStatus(403)
			.bodyJson().extractingPath("$.code").isEqualTo("A005");
	}

	@Test
	@DisplayName("US2-AC1 역할 창의 권한은 관리 메뉴 트리로 보이고, 메뉴마다 그 프로그램의 권한이 있다")
	void permissionTreeFollowsAdminMenus() {
		MvcTestResult result = asAdmin(mvc.get().uri(ROLES + "/permission-tree"));

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$.menus[*].name").asArray().containsExactly("운영", "설정");
		assertThat(result).bodyJson().extractingPath("$.menus[*].children[*].name").asArray()
			.containsExactly("사용자 관리", "공지 관리", "접속 이력", "역할 관리", "메뉴 관리", "공통코드");
		assertThat(result).bodyJson().extractingPath("$.menus[*].children[*].permission.key").asArray()
			.containsExactly("USER_MANAGE", "NOTICE_MANAGE", "LOGIN_HISTORY_READ", "ROLE_MANAGE", "MENU_MANAGE",
				"CODE_MANAGE");
		assertThat(result).bodyJson().extractingPath("$.menus[0].children[2].permission.name").isEqualTo("접속 이력 조회");
		assertThat(result).bodyJson().extractingPath("$.unlisted.length()").isEqualTo(0);
	}

	@Test
	@DisplayName("US2-AC2 접속 이력만 체크해 저장한 역할은 그 권한 하나만 갖고, 목록의 권한 수는 1이다")
	void savedRoleHasOnlyCheckedPermission() {
		assertThat(asAdmin(mvc.get().uri(ROLES + "/" + securityRoleId))).bodyJson().extractingPath("$.permissions")
			.asArray().containsExactly("LOGIN_HISTORY_READ");
		assertThat(asAdmin(mvc.get().uri(ROLES))).bodyJson().extractingPath("$[0].permissions.length()")
			.isEqualTo(1);
	}

	@Test
	@DisplayName("US2-AC3 공통코드 메뉴를 지우면 공통코드는 '메뉴에 없는 프로그램'으로 고를 수 있다")
	void deletedMenuProgramIsUnlisted() {
		Number codeMenuId = JsonPath.<List<Number>>read(body(asAdmin(mvc.get().uri("/api/v1/admin/menus"))),
			"$..[?(@.name == '공통코드')].id").getFirst();
		assertThat(asAdmin(mvc.delete().uri("/api/v1/admin/menus/" + codeMenuId))).hasStatus(204);

		MvcTestResult result = asAdmin(mvc.get().uri(ROLES + "/permission-tree"));

		assertThat(result).bodyJson().extractingPath("$.menus..name").asArray().doesNotContain("공통코드");
		assertThat(result).bodyJson().extractingPath("$.unlisted[*].programKey").asArray()
			.containsExactly("ADMIN_CODES");
		assertThat(result).bodyJson().extractingPath("$.unlisted[0].permission.key").isEqualTo("CODE_MANAGE");
	}

	@Test
	@DisplayName("US3-AC1 사용자별 역할 탭은 관리자와 역할이 있는 사용자만 보이고, 역할이 없는 사용자는 보이지 않는다")
	void roleUsersShowAdminAndUsersWithRoles() {
		testUsers.create(OPERATOR2_ID, "operator2", "operator123!", Role.USER);

		MvcTestResult result = asAdmin(mvc.get().uri(ROLES + "/users"));

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$.content[*].loginId").asArray()
			.containsExactly("admin", "operator");
		assertThat(result).bodyJson().extractingPath("$.content[0].role").isEqualTo("ADMIN");
		assertThat(result).bodyJson().extractingPath("$.content[1].accessRoles[*].name").asArray()
			.containsExactly("보안 담당");
	}

	@Test
	@DisplayName("US3-AC2 아이디로 검색하면 역할이 없는 사용자도 찾고 업무 역할은 비어 있다")
	void searchFindsUsersWithoutRoles() {
		testUsers.create(OPERATOR2_ID, "operator2", "operator123!", Role.USER);

		MvcTestResult result = asAdmin(mvc.get().uri(ROLES + "/users").param("keyword", "operator2"));

		assertThat(result).bodyJson().extractingPath("$.content[*].loginId").asArray().containsExactly("operator2");
		assertThat(result).bodyJson().extractingPath("$.content[0].accessRoles.length()").isEqualTo(0);
	}

	@Test
	@DisplayName("US3-AC3 검색한 운영자에게 역할을 주면 다음 요청부터 접속 이력을 쓰고 기본 목록에도 보인다")
	void givenRoleAppliesOnNextRequest() {
		testUsers.create(OPERATOR2_ID, "operator2", "operator123!", Role.USER);
		assertThat(as(OPERATOR2_ID, mvc.get().uri(LOGIN_HISTORIES))).hasStatus(403);

		giveRoles(OPERATOR2_ID, securityRoleId);

		assertThat(as(OPERATOR2_ID, mvc.get().uri(LOGIN_HISTORIES))).hasStatus(200);
		assertThat(asAdmin(mvc.get().uri(ROLES + "/users"))).bodyJson().extractingPath("$.content[*].loginId")
			.asArray().containsExactly("admin", "operator", "operator2");
	}

	@Test
	@DisplayName("Edge 관리자에게는 업무 역할을 주거나 빼지 않는다(B001)")
	void adminGetsNoAccessRole() {
		assertThat(asAdmin(mvc.put().uri("/api/v1/admin/users/2/roles").contentType(MediaType.APPLICATION_JSON)
			.content("{\"roleIds\":[" + securityRoleId + "]}")))
			.hasStatus(409).bodyJson().extractingPath("$.code").isEqualTo("B001");
		assertThat(addMember(securityRoleId, 2L)).hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("B001");
	}

	@Test
	@DisplayName("US4-AC1 역할의 구성원을 열면 그 역할을 가진 운영자가 아이디와 이름으로 보인다")
	void membersOfRole() {
		testUsers.create(OPERATOR2_ID, "operator2", "operator123!", Role.USER);
		giveRoles(OPERATOR2_ID, securityRoleId);

		MvcTestResult result = asAdmin(mvc.get().uri(ROLES + "/" + securityRoleId + "/members"));

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$[*].loginId").asArray()
			.containsExactly("operator", "operator2");
		assertThat(result).bodyJson().extractingPath("$[0].name").isEqualTo("operator");
	}

	@Test
	@DisplayName("US4-AC2 다른 운영자를 구성원으로 더하면 구성원이 늘고 그 운영자는 다음 요청부터 접속 이력을 쓴다")
	void addedMemberGetsPermission() {
		testUsers.create(OPERATOR2_ID, "operator2", "operator123!", Role.USER);
		testUsers.create(OPERATOR3_ID, "operator3", "operator123!", Role.USER);
		giveRoles(OPERATOR2_ID, securityRoleId);

		assertThat(addMember(securityRoleId, OPERATOR3_ID)).hasStatus(204);
		assertThat(addMember(securityRoleId, OPERATOR3_ID)).as("이미 있으면 그대로 둔다").hasStatus(204);

		assertThat(asAdmin(mvc.get().uri(ROLES + "/" + securityRoleId + "/members"))).bodyJson()
			.extractingPath("$.length()").isEqualTo(3);
		assertThat(as(OPERATOR3_ID, mvc.get().uri(LOGIN_HISTORIES))).hasStatus(200);
	}

	@Test
	@DisplayName("US4-AC3 구성원을 빼면 그 운영자는 다음 요청부터 접속 이력을 쓰지 못한다")
	void removedMemberLosesPermission() {
		assertThat(asAdmin(mvc.delete().uri(ROLES + "/" + securityRoleId + "/members/" + OPERATOR_ID)))
			.hasStatus(204);

		assertThat(asOperator(mvc.get().uri(LOGIN_HISTORIES))).hasStatus(403)
			.bodyJson().extractingPath("$.code").isEqualTo("A005");
	}

	@Test
	@DisplayName("Edge 역할 관리 권한만 받은 운영자는 자기가 갖지 않은 권한의 역할에 구성원을 더할 수 없다(A005)")
	void operatorCannotManageRoleBeyondOwnPermissions() {
		testUsers.create(OPERATOR2_ID, "operator2", "operator123!", Role.USER);
		Number codes = createRole("CODES", "코드 담당", "CODE_MANAGE");
		Number roles = createRole("ROLES", "역할 담당", "ROLE_MANAGE");
		giveRoles(OPERATOR_ID, securityRoleId, roles);

		assertThat(asOperator(mvc.get().uri(ROLES + "/users"))).as("사용자 관리 권한 없이 사용자를 찾는다")
			.hasStatus(200);
		assertThat(asOperator(mvc.post().uri(ROLES + "/" + codes + "/members").contentType(MediaType.APPLICATION_JSON)
			.content("{\"userId\":" + OPERATOR2_ID + "}")))
			.hasStatus(403).bodyJson().extractingPath("$.code").isEqualTo("A005");
	}

	private Number createRole(String code, String name, String permission) {
		MvcTestResult role = asAdmin(mvc.post().uri(ROLES).contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"" + code + "\",\"name\":\"" + name + "\",\"permissions\":[\"" + permission
				+ "\"]}"));
		assertThat(role).hasStatus(201);
		return JsonPath.read(body(role), "$.id");
	}

	private void giveRoles(long userId, Number... roleIds) {
		String ids = String.join(",", Arrays.stream(roleIds).map(String::valueOf).toList());
		assertThat(asAdmin(mvc.put().uri("/api/v1/admin/users/" + userId + "/roles")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"roleIds\":[" + ids + "]}")))
			.hasStatus2xxSuccessful();
	}

	private MvcTestResult addMember(Number roleId, long userId) {
		return asAdmin(mvc.post().uri(ROLES + "/" + roleId + "/members").contentType(MediaType.APPLICATION_JSON)
			.content("{\"userId\":" + userId + "}"));
	}

	private MvcTestResult as(long userId, MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(userId, "USER")).exchange();
	}

	private MvcTestResult asAdmin(MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(2L, "ADMIN")).exchange();
	}

	private MvcTestResult asOperator(MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(OPERATOR_ID, "USER")).exchange();
	}

	private String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
