package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;

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
 * 권한 관리(운영자 역할, 최고 관리자 역할). 스펙 specs/003-admin-roles의 수용 시나리오다
 * 2026-11-21: 운영자는 관리자 계정(ADMIN)이고, 회원(USER)은 역할을 받지 않는다. 모든 권한은 최고 관리자 역할이 준다 (research R21)
 */
class AdminRolesAcceptanceTest extends AcceptanceTest {

	private static final long OPERATOR_ID = 10L;
	private static final long OPERATOR2_ID = 11L;
	private static final long OPERATOR3_ID = 12L;
	private static final long MEMBER_ID = 20L;
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
		testUsers.create(OPERATOR_ID, "operator", "operator123!", Role.ADMIN);

		MvcTestResult role = asAdmin(mvc.post().uri("/api/v1/admin/roles").contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"SECURITY\",\"name\":\"보안 담당\",\"permissions\":[\"LOGIN_HISTORY_READ\"]}"));
		assertThat(role).hasStatus(201);
		securityRoleId = JsonPath.read(body(role), "$.id");

		giveRoles(OPERATOR_ID, securityRoleId);
	}

	@Test
	// moa (2026-11-22): 권한 없는 관리 메뉴도 흐리게 보인다 (specs/001-admin-menus FR-002)
	@DisplayName("US1-AC1 접속 이력 조회 역할만 받은 운영자는 관리 메뉴를 모두 보고, 접속 이력만 들어갈 수 있다")
	void operatorSeesOnlyLoginHistoryMenu() {
		MvcTestResult result = asOperator(mvc.get().uri("/api/v1/menus/me"));

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$[*].name").asArray().containsExactly("운영", "설정");
		assertThat(result).bodyJson().extractingPath("$..[?(@.accessible == true)].name").asArray()
			.containsExactlyInAnyOrder("운영", "접속 이력");
		assertThat(result).bodyJson().extractingPath("$..[?(@.accessible == false)].name").asArray()
			.contains("사용자 관리", "설정", "권한 관리");
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
	@DisplayName("US3-AC1 역할 창의 권한은 관리 메뉴 트리로 보이고, 메뉴마다 그 연결 화면의 권한이 있다")
	void permissionTreeFollowsAdminMenus() {
		MvcTestResult result = asAdmin(mvc.get().uri(ROLES + "/permission-tree"));

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$.menus[*].name").asArray().containsExactly("운영", "설정");
		assertThat(result).bodyJson().extractingPath("$.menus[*].children[*].name").asArray()
			.containsExactly("사용자 관리", "공지 관리", "접속 이력", "파기 기록", "권한 관리", "메뉴 관리", "코드 관리");
		assertThat(result).bodyJson().extractingPath("$.menus[*].children[*].permission.key").asArray()
			.containsExactly("USER_MANAGE", "NOTICE_MANAGE", "LOGIN_HISTORY_READ", "PURGE_LOG_READ", "ROLE_MANAGE",
				"MENU_MANAGE",
				"CODE_MANAGE");
		assertThat(result).bodyJson().extractingPath("$.menus[0].children[2].permission.name").isEqualTo("접속 이력 조회");
		assertThat(result).bodyJson().extractingPath("$.unlisted.length()").isEqualTo(0);
	}

	@Test
	@DisplayName("US3-AC2 접속 이력만 체크해 저장한 역할은 그 권한 하나만 갖고, 목록의 권한 수는 1이다")
	void savedRoleHasOnlyCheckedPermission() {
		assertThat(asAdmin(mvc.get().uri(ROLES + "/" + securityRoleId))).bodyJson().extractingPath("$.permissions")
			.asArray().containsExactly("LOGIN_HISTORY_READ");
		assertThat(asAdmin(mvc.get().uri(ROLES))).bodyJson()
			.extractingPath("$[?(@.code == 'SECURITY')].permissionNames[*]").asArray().containsExactly("접속 이력 조회");
	}

	@Test
	@DisplayName("US3-AC3 코드 관리 메뉴를 지우면 코드 관리는 '메뉴에 없는 연결 화면'으로 고를 수 있다")
	void deletedMenuScreenIsUnlisted() {
		Number codeMenuId = JsonPath.<List<Number>>read(body(asAdmin(mvc.get().uri("/api/v1/admin/menus"))),
			"$..[?(@.name == '코드 관리')].id").getFirst();
		assertThat(asAdmin(mvc.delete().uri("/api/v1/admin/menus/" + codeMenuId))).hasStatus(204);

		MvcTestResult result = asAdmin(mvc.get().uri(ROLES + "/permission-tree"));

		assertThat(result).bodyJson().extractingPath("$.menus..name").asArray().doesNotContain("코드 관리");
		assertThat(result).bodyJson().extractingPath("$.unlisted[*].screenKey").asArray()
			.containsExactly("ADMIN_CODES");
		assertThat(result).bodyJson().extractingPath("$.unlisted[0].permission.key").isEqualTo("CODE_MANAGE");
	}

	@Test
	@DisplayName("US4-AC4 역할 둘을 골라 지우면 둘 다 없어지고 구성원에게서도 빠진다. 다룰 수 없는 역할이 섞이면 아무것도 지우지 않는다(A005)")
	void bulkDeleteRoles() {
		Number codes = createRole("CODES", "코드 담당", "CODE_MANAGE");
		Number roles = createRole("ROLES", "역할 담당", "ROLE_MANAGE");
		giveRoles(OPERATOR_ID, securityRoleId, roles);

		// 운영자는 역할 관리·접속 이력만 가져 코드 담당(CODE_MANAGE)을 다룰 수 없다
		assertThat(asOperator(mvc.post().uri(ROLES + "/bulk-delete").contentType(MediaType.APPLICATION_JSON)
			.content("{\"ids\":[" + securityRoleId + "," + codes + "]}")))
			.hasStatus(403).bodyJson().extractingPath("$.code").isEqualTo("A005");
		assertThat(asAdmin(mvc.get().uri(ROLES))).bodyJson().extractingPath("$.length()").isEqualTo(4);

		assertThat(asAdmin(mvc.post().uri(ROLES + "/bulk-delete").contentType(MediaType.APPLICATION_JSON)
			.content("{\"ids\":[" + securityRoleId + "," + codes + "]}")))
			.hasStatus(204);
		assertThat(asAdmin(mvc.get().uri(ROLES))).bodyJson().extractingPath("$[*].code").asArray()
			.containsExactly("SUPER_ADMIN", "ROLES");
		assertThat(asOperator(mvc.get().uri(LOGIN_HISTORIES))).hasStatus(403);
	}

	@Test
	@DisplayName("FR-002 회원(USER)에게는 역할을 줄 수 없다(B001). 2026-11-21 전에는 관리자(ADMIN)에게 주면 B001이었다")
	void adminGetsNoAccessRole() {
		testUsers.create(MEMBER_ID, "member", "member1234!", Role.USER);
		assertThat(asAdmin(mvc.put().uri("/api/v1/admin/users/" + MEMBER_ID + "/roles")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"roleIds\":[" + securityRoleId + "]}")))
			.hasStatus(409).bodyJson().extractingPath("$.code").isEqualTo("B001");
	}

	@Test
	@DisplayName("US5-AC1 검색하지 않은 구성원은 구성원만 보이고 20명을 넘으면 쪽으로 나뉜다")
	void membersArePaged() {
		for (long id = 100; id < 120; id++) {
			testUsers.create(id, "op" + id, "operator123!", Role.ADMIN);
		}
		addMembers(securityRoleId, LongStream.range(100, 120).boxed().toList());
		testUsers.create(OPERATOR2_ID, "operator2", "operator123!", Role.ADMIN);

		MvcTestResult result = asAdmin(mvc.get().uri(ROLES + "/" + securityRoleId + "/members").param("size", "20"));

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$.totalElements").isEqualTo(21);
		assertThat(result).bodyJson().extractingPath("$.content.length()").isEqualTo(20);
		assertThat(result).bodyJson().extractingPath("$.content[*].member").asArray().containsOnly(true);
		assertThat(result).bodyJson().extractingPath("$.content[*].loginId").asArray().doesNotContain("operator2");
	}

	@Test
	@DisplayName("US5-AC2 검색 결과에 구성원 여부가 보이고, 구성원이 아닌 둘을 한꺼번에 더하면 다음 요청부터 접속 이력을 쓴다")
	void searchAndAddMembers() {
		testUsers.create(OPERATOR2_ID, "operator2", "operator123!", Role.ADMIN);
		testUsers.create(OPERATOR3_ID, "operator3", "operator123!", Role.ADMIN);

		MvcTestResult found = asAdmin(
			mvc.get().uri(ROLES + "/" + securityRoleId + "/members").param("keyword", "operator"));
		assertThat(found).bodyJson().extractingPath("$.content[*].loginId").asArray()
			.containsExactly("operator", "operator2", "operator3");
		assertThat(found).bodyJson().extractingPath("$.content[*].member").asArray().containsExactly(true, false,
			false);

		assertThat(addMembers(securityRoleId, List.of(OPERATOR2_ID, OPERATOR3_ID, OPERATOR_ID))).hasStatus(204);
		assertThat(as(OPERATOR2_ID, mvc.get().uri(LOGIN_HISTORIES))).hasStatus(200);
		assertThat(as(OPERATOR3_ID, mvc.get().uri(LOGIN_HISTORIES))).hasStatus(200);
		assertThat(asAdmin(mvc.get().uri(ROLES + "/" + securityRoleId + "/members"))).bodyJson()
			.extractingPath("$.totalElements").isEqualTo(3);
	}

	@Test
	@DisplayName("US5-AC3 구성원 둘을 한꺼번에 빼면 다음 요청부터 접속 이력을 쓰지 못한다")
	void removeMembers() {
		testUsers.create(OPERATOR2_ID, "operator2", "operator123!", Role.ADMIN);
		addMembers(securityRoleId, List.of(OPERATOR2_ID));

		assertThat(asAdmin(mvc.post().uri(ROLES + "/" + securityRoleId + "/members/remove")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"userIds\":[" + OPERATOR_ID + "," + OPERATOR2_ID + "]}")))
			.hasStatus(204);

		assertThat(asOperator(mvc.get().uri(LOGIN_HISTORIES))).hasStatus(403)
			.bodyJson().extractingPath("$.code").isEqualTo("A005");
		assertThat(as(OPERATOR2_ID, mvc.get().uri(LOGIN_HISTORIES))).hasStatus(403);
	}

	@Test
	@DisplayName("US5-AC4 회원이나 최고 관리자가 섞인 더하기는 아무도 더하지 않는다(B001). 검색에 회원은 없고 최고 관리자는 표시된다")
	void addingAdminFailsWholly() {
		testUsers.create(OPERATOR2_ID, "operator2", "operator123!", Role.ADMIN);
		testUsers.create(MEMBER_ID, "member", "member1234!", Role.USER);

		MvcTestResult found = asAdmin(mvc.get().uri(ROLES + "/" + securityRoleId + "/members").param("keyword", "m"));
		assertThat(found).bodyJson().extractingPath("$.content[*].loginId").asArray().contains("admin")
			.doesNotContain("member");
		assertThat(found).bodyJson().extractingPath("$.content[?(@.loginId == 'admin')].superAdmin").asArray()
			.containsExactly(true);

		assertThat(addMembers(securityRoleId, List.of(OPERATOR2_ID, MEMBER_ID))).hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("B001");
		assertThat(as(OPERATOR2_ID, mvc.get().uri(LOGIN_HISTORIES))).hasStatus(403);
		assertThat(addMembers(securityRoleId, List.of(OPERATOR2_ID, 2L))).hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("B001");
		assertThat(as(OPERATOR2_ID, mvc.get().uri(LOGIN_HISTORIES))).hasStatus(403);
	}

	@Test
	@DisplayName("Edge 역할 관리 권한만 받은 운영자는 사용자를 찾을 수 있지만, 자기가 갖지 않은 권한의 역할에 구성원을 더할 수 없다(A005)")
	void operatorCannotManageRoleBeyondOwnPermissions() {
		testUsers.create(OPERATOR2_ID, "operator2", "operator123!", Role.ADMIN);
		Number codes = createRole("CODES", "코드 담당", "CODE_MANAGE");
		Number roles = createRole("ROLES", "역할 담당", "ROLE_MANAGE");
		giveRoles(OPERATOR_ID, securityRoleId, roles);

		assertThat(asOperator(mvc.get().uri(ROLES + "/" + codes + "/members").param("keyword", "operator")))
			.as("사용자 관리 권한 없이 사용자를 찾는다").hasStatus(200);
		assertThat(asOperator(mvc.post().uri(ROLES + "/" + codes + "/members").contentType(MediaType.APPLICATION_JSON)
			.content("{\"userIds\":[" + OPERATOR2_ID + "]}")))
			.hasStatus(403).bodyJson().extractingPath("$.code").isEqualTo("A005");
	}

	@Test
	@DisplayName("US1-AC4 역할을 하나도 받지 않은 관리자 계정은 관리 메뉴가 모두 잠겨 보이고 관리 API는 막힌다")
	void adminWithoutRoleHasNothing() {
		testUsers.create(OPERATOR2_ID, "operator2", "operator123!", Role.ADMIN);

		MvcTestResult menus = as(OPERATOR2_ID, mvc.get().uri("/api/v1/menus/me"));
		assertThat(menus).hasStatus(200);
		assertThat(menus).bodyJson().extractingPath("$[*].name").asArray().containsExactly("운영", "설정");
		assertThat(menus).bodyJson().extractingPath("$..[?(@.accessible == true)].name").asArray().isEmpty();
		assertThat(as(OPERATOR2_ID, mvc.get().uri(LOGIN_HISTORIES))).hasStatus(403)
			.bodyJson().extractingPath("$.code").isEqualTo("A005");
	}

	@Test
	@DisplayName("US2-AC1 최고 관리자 역할은 목록 맨 앞에 고정 역할로 있고, 최고 관리자가 그 구성원이다")
	void superAdminRoleComesFirst() {
		MvcTestResult roles = asAdmin(mvc.get().uri(ROLES));

		assertThat(roles).bodyJson().extractingPath("$[0].code").isEqualTo("SUPER_ADMIN");
		assertThat(roles).bodyJson().extractingPath("$[0].fixed").isEqualTo(true);
		assertThat(asAdmin(mvc.get().uri(ROLES + "/" + superRoleId() + "/members"))).bodyJson()
			.extractingPath("$.content[*].loginId").asArray().containsExactly("admin");
	}

	@Test
	@DisplayName("US2-AC2 최고 관리자 역할은 바꾸거나 지울 수 없다(B001). 섞어 지우면 아무것도 지우지 않는다")
	void superAdminRoleIsFixed() {
		Number superRole = superRoleId();

		assertThat(asAdmin(mvc.put().uri(ROLES + "/" + superRole).contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"바꿈\",\"permissions\":[]}"))).hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("B001");
		assertThat(asAdmin(mvc.delete().uri(ROLES + "/" + superRole))).hasStatus(409);
		assertThat(asAdmin(mvc.post().uri(ROLES + "/bulk-delete").contentType(MediaType.APPLICATION_JSON)
			.content("{\"ids\":[" + securityRoleId + "," + superRole + "]}"))).hasStatus(409);
		assertThat(asAdmin(mvc.get().uri(ROLES))).bodyJson().extractingPath("$.length()").isEqualTo(2);
	}

	@Test
	@DisplayName("US2-AC3 최고 관리자는 역할을 고치지 않아도 모든 관리 연결 화면의 권한을 갖는다")
	void superAdminHasEveryPermission() {
		assertThat(asAdmin(mvc.get().uri("/api/v1/users/me/permissions"))).bodyJson().extractingPath("$.permissions")
			.asArray().contains("USER_MANAGE", "ROLE_MANAGE", "LOGIN_HISTORY_READ", "PURGE_LOG_READ", "SAMPLE_MANAGE");
	}

	@Test
	@DisplayName("US2-AC4 최고 관리자 역할의 마지막 구성원은 뺄 수 없다(B001)")
	void lastSuperAdminStays() {
		Number superRole = superRoleId();

		assertThat(asAdmin(mvc.post().uri(ROLES + "/" + superRole + "/members/remove")
			.contentType(MediaType.APPLICATION_JSON).content("{\"userIds\":[2]}"))).hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("B001");
		assertThat(asAdmin(mvc.put().uri("/api/v1/admin/users/2/roles").contentType(MediaType.APPLICATION_JSON)
			.content("{\"roleIds\":[" + securityRoleId + "]}"))).hasStatus(409);
	}

	@Test
	@DisplayName("US2-AC5 권한 관리 권한을 받은 운영자는 최고 관리자 역할의 구성원을 더하거나 뺄 수 없다(A005)")
	void operatorCannotTouchSuperAdminRole() {
		Number roles = createRole("ROLES", "역할 담당", "ROLE_MANAGE");
		giveRoles(OPERATOR_ID, securityRoleId, roles);
		testUsers.create(OPERATOR2_ID, "operator2", "operator123!", Role.ADMIN);

		assertThat(asOperator(mvc.post().uri(ROLES + "/" + superRoleId() + "/members")
			.contentType(MediaType.APPLICATION_JSON).content("{\"userIds\":[" + OPERATOR2_ID + "]}")))
			.hasStatus(403).bodyJson().extractingPath("$.code").isEqualTo("A005");
		assertThat(asOperator(mvc.get().uri(ROLES))).bodyJson()
			.extractingPath("$[?(@.code == 'SUPER_ADMIN')].blockReason").asArray().containsExactly("SUPER_ADMIN_ONLY");
	}

	@Test
	@DisplayName("FR-008 사용자 관리 권한이 든 역할은 최고 관리자만 만든다. 사용자·권한 관리 운영자도 A005")
	void onlySuperAdminHandsOutUserManage() {
		String manager = operator(OPERATOR2_ID, "manager", "USER_MANAGE", "ROLE_MANAGE");

		assertThat(mvc.post().uri(ROLES).header(HttpHeaders.AUTHORIZATION, manager)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"USERS\",\"name\":\"계정 담당\",\"permissions\":[\"USER_MANAGE\"]}"))
			.hasStatus(403).bodyJson().extractingPath("$.code").isEqualTo("A005");
		assertThat(createRole("USERS", "계정 담당", "USER_MANAGE")).isNotNull();
	}

	@Test
	@DisplayName("FR-010 다른 역할을 가진 운영자를 최고 관리자 역할에 더하면 다른 역할은 빠진다")
	void superAdminDropsOtherRoles() {
		assertThat(addMembers(superRoleId(), List.of(OPERATOR_ID))).hasStatus(204);

		assertThat(asAdmin(mvc.get().uri("/api/v1/admin/users/" + OPERATOR_ID + "/roles"))).bodyJson()
			.extractingPath("$[*].code").asArray().containsExactly("SUPER_ADMIN");
		assertThat(asOperator(mvc.get().uri("/api/v1/admin/users"))).hasStatus(200);
	}

	private Number superRoleId() {
		return JsonPath.<List<Number>>read(body(asAdmin(mvc.get().uri(ROLES))), "$[?(@.code == 'SUPER_ADMIN')].id")
			.getFirst();
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

	private MvcTestResult addMembers(Number roleId, List<Long> userIds) {
		return asAdmin(mvc.post().uri(ROLES + "/" + roleId + "/members").contentType(MediaType.APPLICATION_JSON)
			.content("{\"userIds\":" + userIds + "}"));
	}

	private MvcTestResult as(long userId, MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(userId, "ADMIN")).exchange();
	}

	private MvcTestResult asAdmin(MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, superAdmin()).exchange();
	}

	private MvcTestResult asOperator(MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(OPERATOR_ID, "ADMIN")).exchange();
	}

	private String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
