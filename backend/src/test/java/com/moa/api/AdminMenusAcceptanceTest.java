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
import com.moa.support.AcceptanceTest;
import com.moa.support.MenuSeed;

/**
 * 관리 메뉴와 접근. 스펙 specs/001-admin-menus의 수용 시나리오다
 */
class AdminMenusAcceptanceTest extends AcceptanceTest {

	private static final String MY_MENUS = "/api/v1/menus/me";
	private static final String USER_MENUS = "/api/v1/menus/user";
	private static final String ADMIN_MENUS = "/api/v1/admin/menus";
	private static final List<String> ADMIN_LIST_APIS = List.of(
		"/api/v1/admin/users",
		ADMIN_MENUS,
		"/api/v1/admin/code-groups",
		"/api/v1/admin/login-histories",
		"/api/v1/admin/roles",
		"/api/v1/admin/notices");

	@Autowired
	private MenuSeed menuSeed;

	/** DatabaseCleaner가 시드까지 지우므로 처음 구성으로 되돌리고 시작한다 */
	@BeforeEach
	void restoreInitialMenus() {
		menuSeed.restore();
	}

	@Test
	@DisplayName("US1-AC1 관리자는 관리 메뉴 6개를 최상위에서 정해진 순서로 본다")
	void adminSeesMenusInOrder() {
		MvcTestResult result = asAdmin(mvc.get().uri(MY_MENUS));

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$[*].name").asArray()
			.containsExactly("사용자 관리", "메뉴 관리", "코드 관리", "접속 이력", "역할 관리", "공지 관리");
		assertThat(result).bodyJson().extractingPath("$[*].children[*]").asArray().isEmpty();
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
			.content("{\"parentId\":null,\"name\":\"새 메뉴\",\"path\":\"/new\",\"sortOrder\":10,\"permission\":null}")))
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
			.containsExactlyInAnyOrder("사용자 관리", "메뉴 관리", "코드 관리", "접속 이력", "역할 관리", "공지 관리");
		assertThat(result).bodyJson().extractingPath("$[?(@.audience == 'USER')].name").asArray()
			.containsExactlyInAnyOrder("홈", "공지");
	}

	@Test
	@DisplayName("US3-AC2 권한 없이 더한 사용자 메뉴는 일반 회원과 로그인하지 않은 사람의 사용자 셸에 보이고 관리 셸에는 없다")
	void userMenuIsVisibleToEveryone() {
		createMenu("이벤트", "/events", "USER");

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
		Number id = createMenu("이벤트", "/events", "USER");

		assertThat(asAdmin(mvc.put().uri(ADMIN_MENUS + "/" + id).contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"이벤트\",\"path\":\"/events\",\"sortOrder\":30,\"enabled\":false}")))
			.hasStatus(200);

		assertThat(mvc.get().uri(USER_MENUS).exchange()).bodyJson().extractingPath("$[*].name").asArray()
			.containsExactly("홈", "공지");
	}

	@Test
	@DisplayName("US3-AC4 권한 없이 더한 관리 메뉴는 일반 회원에게 보이지 않는다")
	void adminMenuWithoutPermissionIsHiddenFromUser() {
		createMenu("운영 도구", "/admin/tools", "ADMIN");

		assertThat(asUser(mvc.get().uri(MY_MENUS))).bodyJson().extractingPath("$.length()").isEqualTo(0);
		assertThat(mvc.get().uri(USER_MENUS).exchange()).bodyJson().extractingPath("$[*].name").asArray()
			.doesNotContain("운영 도구");
		assertThat(asAdmin(mvc.get().uri(MY_MENUS))).bodyJson().extractingPath("$[*].name").asArray()
			.contains("운영 도구");
	}

	/** 메뉴를 만들고 ID를 돌려준다 */
	private Number createMenu(String name, String path, String audience) {
		MvcTestResult result = asAdmin(mvc.post().uri(ADMIN_MENUS).contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"%s\",\"path\":\"%s\",\"sortOrder\":30,\"audience\":\"%s\"}"
				.formatted(name, path, audience)));
		assertThat(result).hasStatus(201);
		try {
			return JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id");
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
}
