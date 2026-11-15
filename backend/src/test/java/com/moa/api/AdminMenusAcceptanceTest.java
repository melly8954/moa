package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
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
import com.moa.support.TestPrograms;
import com.moa.support.TestUsers;

/**
 * 관리 메뉴와 접근. 스펙 specs/001-admin-menus의 수용 시나리오다
 */
class AdminMenusAcceptanceTest extends AcceptanceTest {

	private static final String MY_MENUS = "/api/v1/menus/me";
	private static final String USER_MENUS = "/api/v1/menus/user";
	private static final String ADMIN_MENUS = "/api/v1/admin/menus";
	private static final String PROGRAMS = "/api/v1/admin/programs";
	private static final List<String> ADMIN_LIST_APIS = List.of(
		"/api/v1/admin/users",
		ADMIN_MENUS,
		"/api/v1/admin/code-groups",
		"/api/v1/admin/login-histories",
		"/api/v1/admin/roles",
		"/api/v1/admin/notices");
	private static final long OPERATOR_ID = 10L;

	@Autowired
	private MenuSeed menuSeed;

	@Autowired
	private TestUsers testUsers;

	/** DatabaseCleaner가 시드까지 지우므로 처음 구성으로 되돌리고 시작한다 */
	@BeforeEach
	void restoreInitialMenus() {
		menuSeed.restore();
	}

	@Test
	@DisplayName("US1-AC1 관리자는 관리 메뉴 6개를 운영·설정 묶음 아래에서 정해진 순서로 본다")
	void adminSeesMenusInOrder() {
		MvcTestResult result = asAdmin(mvc.get().uri(MY_MENUS));

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$[*].name").asArray().containsExactly("운영", "설정");
		assertThat(result).bodyJson().extractingPath("$[0].children[*].name").asArray()
			.containsExactly("사용자 관리", "공지 관리", "접속 이력");
		assertThat(result).bodyJson().extractingPath("$[1].children[*].name").asArray()
			.containsExactly("역할 관리", "메뉴 관리", "공통코드");
		assertThat(result).bodyJson().extractingPath("$[*].children[*].path").asArray()
			.containsExactly("/admin/users", "/admin/notices", "/admin/login-histories", "/admin/roles", "/admin/menus",
				"/admin/codes");
	}

	@Test
	@DisplayName("US1-AC2 관리자는 관리 화면 6개의 목록을 연다")
	void adminOpensEveryList() {
		for (String api : ADMIN_LIST_APIS) {
			assertThat(asAdmin(mvc.get().uri(api))).as(api).hasStatus(200);
		}
	}

	@Test
	@DisplayName("US2-AC1 일반 회원에게는 관리 메뉴가 보이지 않는다")
	void userSeesNoAdminMenu() {
		MvcTestResult result = asUser(mvc.get().uri(MY_MENUS));

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$.length()").isEqualTo(0);
	}

	@Test
	@DisplayName("US2-AC2 일반 회원이 관리 기능을 쓰려 하면 권한 없음(A005)으로 막힌다")
	void userIsForbidden() {
		for (String api : ADMIN_LIST_APIS) {
			assertThat(asUser(mvc.get().uri(api))).as(api).hasStatus(403)
				.bodyJson().extractingPath("$.code").isEqualTo("A005");
		}

		assertThat(asUser(mvc.post().uri(ADMIN_MENUS).contentType(MediaType.APPLICATION_JSON)
			.content(json(null, "새 메뉴", "ADMIN_CODES", "ADMIN", null))))
			.hasStatus(403)
			.bodyJson().extractingPath("$.code").isEqualTo("A005");
	}

	@Test
	@DisplayName("US2-AC3 로그인하지 않으면 로그인 필요(A001)로 막힌다")
	void anonymousIsUnauthorized() {
		for (String api : ADMIN_LIST_APIS) {
			assertThat(mvc.get().uri(api)).as(api).hasStatus(401)
				.bodyJson().extractingPath("$.code").isEqualTo("A001");
		}
	}

	@Test
	@DisplayName("US3-AC1 메뉴 관리에서 관리 메뉴와 사용자 메뉴(홈, 공지)가 대상으로 나뉘어 보인다")
	void adminSeesMenusByAudience() {
		MvcTestResult result = asAdmin(mvc.get().uri(ADMIN_MENUS));

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$[?(@.audience == 'ADMIN')].name").asArray()
			.containsExactlyInAnyOrder("운영", "설정");
		assertThat(result).bodyJson().extractingPath("$[?(@.audience == 'ADMIN')].children[*].name").asArray()
			.containsExactlyInAnyOrder("사용자 관리", "메뉴 관리", "공통코드", "접속 이력", "역할 관리", "공지 관리");
		assertThat(result).bodyJson().extractingPath("$[?(@.audience == 'USER')].name").asArray()
			.containsExactlyInAnyOrder("홈", "공지");
	}

	@Test
	@DisplayName("US3-AC2 공개 범위 '비회원 포함'으로 더한 사용자 메뉴는 일반 회원과 로그인하지 않은 사람의 사용자 셸에 보이고 관리 셸에는 없다")
	void publicUserMenuIsVisibleToEveryone() {
		createMenu(null, "이벤트", "NOTICES", "USER", "PUBLIC");

		for (MvcTestResult result : List.of(mvc.get().uri(USER_MENUS).exchange(), asUser(mvc.get().uri(USER_MENUS)))) {
			assertThat(result).hasStatus(200);
			assertThat(result).bodyJson().extractingPath("$[*].name").asArray()
				.containsExactly("홈", "공지", "이벤트");
		}
		assertThat(asAdmin(mvc.get().uri(MY_MENUS))).bodyJson().extractingPath("$[*].name").asArray()
			.doesNotContain("이벤트", "홈", "공지");
	}

	@Test
	@DisplayName("US3-AC3 관리자가 사용자 메뉴를 끄면 사용자 셸에서 빠진다")
	void disabledUserMenuIsHidden() {
		Number id = createMenu(null, "이벤트", "NOTICES", "USER", "PUBLIC");

		assertThat(asAdmin(mvc.put().uri(ADMIN_MENUS + "/" + id).contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"이벤트\",\"programKey\":\"NOTICES\",\"visibility\":\"PUBLIC\",\"sortOrder\":30,"
				+ "\"enabled\":false}")))
			.hasStatus(200);

		assertThat(mvc.get().uri(USER_MENUS).exchange()).bodyJson().extractingPath("$[*].name").asArray()
			.containsExactly("홈", "공지");
	}

	@Test
	@DisplayName("US3-AC4 관리자가 더한 관리 메뉴는 일반 회원에게 보이지 않는다")
	void adminMenuIsHiddenFromUser() {
		createMenu(null, "코드 도구", "ADMIN_CODES", "ADMIN", null);

		assertThat(asUser(mvc.get().uri(MY_MENUS))).bodyJson().extractingPath("$.length()").isEqualTo(0);
		assertThat(mvc.get().uri(USER_MENUS).exchange()).bodyJson().extractingPath("$[*].name").asArray()
			.doesNotContain("코드 도구");
		assertThat(asAdmin(mvc.get().uri(MY_MENUS))).bodyJson().extractingPath("$[*].name").asArray()
			.contains("코드 도구");
	}

	@Test
	@DisplayName("US3-AC5 공개 범위 '회원만'으로 더한 사용자 메뉴는 로그인하지 않은 사람에게는 없고 일반 회원에게는 보인다")
	void memberUserMenuIsHiddenFromAnonymous() {
		createMenu(null, "내 활동", TestPrograms.MEMBER_ONLY, "USER", "MEMBER");

		assertThat(mvc.get().uri(USER_MENUS).exchange()).bodyJson().extractingPath("$[*].name").asArray()
			.containsExactly("홈", "공지");
		assertThat(asUser(mvc.get().uri(USER_MENUS))).bodyJson().extractingPath("$[*].name").asArray()
			.containsExactly("홈", "공지", "내 활동");
	}

	@Test
	@DisplayName("US4-AC1 관리자는 등록된 프로그램 목록을 이름·경로·대상·권한과 함께 보고, 목록을 고치는 API는 없다")
	void adminSeesPrograms() {
		MvcTestResult result = asAdmin(mvc.get().uri(PROGRAMS));

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$[?(@.key =~ /^(?!TEST_).*/)].key").asArray()
			.containsExactly("ADMIN_USERS", "ADMIN_MENUS", "ADMIN_CODES", "ADMIN_LOGIN_HISTORIES", "ADMIN_ROLES",
				"ADMIN_NOTICES", "HOME", "NOTICES");
		assertThat(result).bodyJson().extractingPath("$[?(@.key == 'ADMIN_CODES')].permission.name").asArray()
			.containsExactly("공통코드 관리");
		assertThat(result).bodyJson().extractingPath("$[?(@.key == 'NOTICES')].path").asArray()
			.containsExactly("/notices");
		assertThat(result).bodyJson().extractingPath("$[?(@.key == 'NOTICES')].signInRequired").asArray()
			.containsExactly(false);

		assertThat(asUser(mvc.get().uri(PROGRAMS))).hasStatus(403)
			.bodyJson().extractingPath("$.code").isEqualTo("A005");
		assertThat(asAdmin(mvc.post().uri(PROGRAMS).contentType(MediaType.APPLICATION_JSON).content("{}")))
			.hasStatus(405);
	}

	@Test
	@DisplayName("US4-AC2 관리 메뉴는 관리 프로그램만 고르고, 경로와 권한은 고른 프로그램을 따른다")
	void adminMenuFollowsProgram() {
		assertThat(post(json(null, "공지", "NOTICES", "ADMIN", null))).hasStatus(400)
			.bodyJson().extractingPath("$.code").isEqualTo("V001");

		MvcTestResult created = post(json(null, "코드 도구", "ADMIN_CODES", "ADMIN", null));

		assertThat(created).hasStatus(201);
		assertThat(created).bodyJson().extractingPath("$.program.path").isEqualTo("/admin/codes");
		assertThat(created).bodyJson().extractingPath("$.program.permissionName").isEqualTo("공통코드 관리");
		assertThat(created).bodyJson().extractingPath("$.programKey").isEqualTo("ADMIN_CODES");
		assertThat(created).bodyJson().doesNotHavePath("$.path");
		assertThat(created).bodyJson().doesNotHavePath("$.permission");
	}

	@Test
	@DisplayName("US4-AC3 로그인이 필요한 사용자 프로그램은 공개 범위를 '회원만'으로만 고를 수 있다")
	void signInRequiredProgramIsMemberOnly() {
		assertThat(post(json(null, "내 활동", TestPrograms.MEMBER_ONLY, "USER", "PUBLIC"))).hasStatus(400)
			.bodyJson().extractingPath("$.code").isEqualTo("V001");
		assertThat(post(json(null, "내 활동", TestPrograms.MEMBER_ONLY, "USER", "MEMBER"))).hasStatus(201);
	}

	@Test
	@DisplayName("US4-AC4 묶음 메뉴 아래 코드 관리 메뉴는 코드 관리 권한만 받은 운영자에게 묶음과 함께 보인다")
	void groupedProgramMenuIsVisibleToOperator() {
		giveOperatorRole("CODE_MANAGE");
		Number group = createMenu(null, "시스템", null, "ADMIN", null);
		createMenu(group, "코드", "ADMIN_CODES", null, null);

		MvcTestResult result = asOperator(mvc.get().uri(MY_MENUS));

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$[?(@.name == '시스템')].children[*].path").asArray()
			.containsExactly("/admin/codes");
		assertThat(result).bodyJson().extractingPath("$[*].name").asArray()
			.containsExactly("설정", "시스템");
	}

	@Test
	@DisplayName("US4-AC5 프로그램 없이 묶음 메뉴를 만들 수 있고, 보이는 하위 메뉴가 없으면 사이드바에 보이지 않는다")
	void emptyGroupIsHidden() {
		createMenu(null, "빈 묶음", null, "ADMIN", null);

		assertThat(asAdmin(mvc.get().uri(MY_MENUS))).bodyJson().extractingPath("$[*].name").asArray()
			.doesNotContain("빈 묶음");
		assertThat(asAdmin(mvc.get().uri(ADMIN_MENUS))).bodyJson().extractingPath("$[*].name").asArray()
			.contains("빈 묶음");
	}

	@Test
	@DisplayName("US5-AC3 접속 이력을 설정 묶음 안으로 옮기면 다음 조회부터 설정 아래에 보인다")
	void moveMenuIntoAnotherGroup() {
		long settings = menuId("설정");

		assertThat(asAdmin(mvc.put().uri(ADMIN_MENUS + "/order").contentType(MediaType.APPLICATION_JSON)
			.content("[{\"id\":" + menuId("접속 이력") + ",\"parentId\":" + settings + ",\"sortOrder\":40}]")))
			.hasStatus(204);

		MvcTestResult result = asAdmin(mvc.get().uri(MY_MENUS));
		assertThat(result).bodyJson().extractingPath("$[0].children[*].name").asArray()
			.containsExactly("사용자 관리", "공지 관리");
		assertThat(result).bodyJson().extractingPath("$[1].children[*].name").asArray()
			.containsExactly("역할 관리", "메뉴 관리", "공통코드", "접속 이력");
	}

	@Test
	@DisplayName("US5-AC4 메뉴 둘을 골라 사용 안 함으로 바꾸면 둘 다 사이드바에서 빠진다")
	void disableMenusAtOnce() {
		assertThat(asAdmin(mvc.put().uri(ADMIN_MENUS + "/enabled").contentType(MediaType.APPLICATION_JSON)
			.content("{\"ids\":[" + menuId("공지 관리") + "," + menuId("접속 이력") + "],\"enabled\":false}")))
			.hasStatus(204);

		assertThat(asAdmin(mvc.get().uri(MY_MENUS))).bodyJson().extractingPath("$[0].children[*].name").asArray()
			.containsExactly("사용자 관리");
	}

	@Test
	@DisplayName("US5-AC5 하위 메뉴가 있는 묶음과 다른 메뉴를 함께 지우려 하면 아무것도 지워지지 않는다(B001)")
	void bulkDeleteKeepsGroupWithChildren() {
		assertThat(asAdmin(mvc.post().uri(ADMIN_MENUS + "/bulk-delete").contentType(MediaType.APPLICATION_JSON)
			.content("{\"ids\":[" + menuId("운영") + "," + menuId("메뉴 관리") + "]}")))
			.hasStatus(409).bodyJson().extractingPath("$.code").isEqualTo("B001");

		assertThat(asAdmin(mvc.get().uri(ADMIN_MENUS))).bodyJson().extractingPath("$..name").asArray()
			.contains("운영", "메뉴 관리");
	}

	/** 메뉴 관리 트리에서 이름으로 메뉴 ID를 찾는다 */
	private long menuId(String name) {
		List<Number> ids = JsonPath.read(body(asAdmin(mvc.get().uri(ADMIN_MENUS))),
			"$..[?(@.name == '" + name + "')].id");
		return ids.getFirst().longValue();
	}

	/** 운영자 계정을 만들고, 이 권한만 묶은 역할을 준다 */
	private void giveOperatorRole(String permission) {
		testUsers.create(1L, "system", null, Role.USER);
		testUsers.create(2L, "admin", "admin1234!", Role.ADMIN);
		testUsers.create(OPERATOR_ID, "operator", "operator123!", Role.USER);
		MvcTestResult role = asAdmin(mvc.post().uri("/api/v1/admin/roles").contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"OPERATOR\",\"name\":\"운영\",\"permissions\":[\"" + permission + "\"]}"));
		assertThat(role).hasStatus(201);
		assertThat(asAdmin(mvc.put().uri("/api/v1/admin/users/" + OPERATOR_ID + "/roles")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"roleIds\":[" + JsonPath.read(body(role), "$.id") + "]}")))
			.hasStatus2xxSuccessful();
	}

	/** 메뉴를 만들고 ID를 돌려준다 */
	private Number createMenu(Number parentId, String name, String programKey, String audience, String visibility) {
		MvcTestResult result = post(json(parentId, name, programKey, audience, visibility));
		assertThat(result).hasStatus(201);
		return JsonPath.read(body(result), "$.id");
	}

	private MvcTestResult post(String json) {
		return asAdmin(mvc.post().uri(ADMIN_MENUS).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private String json(Number parentId, String name, String programKey, String audience, String visibility) {
		return "{\"parentId\":" + parentId + ",\"name\":\"" + name + "\",\"programKey\":" + quote(programKey)
			+ ",\"sortOrder\":30,\"audience\":" + quote(audience) + ",\"visibility\":" + quote(visibility) + "}";
	}

	private String quote(String value) {
		return value == null ? "null" : "\"" + value + "\"";
	}

	private String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private MvcTestResult asAdmin(MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(2L, "ADMIN")).exchange();
	}

	private MvcTestResult asUser(MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(3L, "USER")).exchange();
	}

	private MvcTestResult asOperator(MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(OPERATOR_ID, "USER")).exchange();
	}
}
