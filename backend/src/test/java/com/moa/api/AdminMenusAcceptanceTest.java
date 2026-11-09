package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.moa.support.AcceptanceTest;

/**
 * 관리 메뉴와 접근. 스펙 specs/001-admin-menus의 수용 시나리오다
 */
class AdminMenusAcceptanceTest extends AcceptanceTest {

	private static final String MY_MENUS = "/api/v1/menus/me";
	private static final String ADMIN_MENUS = "/api/v1/admin/menus";
	private static final List<String> ADMIN_LIST_APIS = List.of(
		"/api/v1/admin/users",
		ADMIN_MENUS,
		"/api/v1/admin/code-groups",
		"/api/v1/admin/login-histories",
		"/api/v1/admin/roles");
	private static final List<String> MENU_SEED_SCRIPTS = List.of(
		"db/migration/V1_7__menus.sql",
		"db/migration/V1_7_1__admin_menus.sql");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/** DatabaseCleaner가 시드까지 지우므로, 마이그레이션 파일의 INSERT 문을 다시 실행해 처음 구성으로 시작한다 */
	@BeforeEach
	void restoreInitialMenus() throws IOException {
		jdbcTemplate.update("delete from menus");
		for (String script : MENU_SEED_SCRIPTS) {
			for (String insert : insertStatementsOf(script)) {
				jdbcTemplate.update(insert);
			}
		}
	}

	@Test
	@DisplayName("US1-AC1 관리자는 관리 메뉴 5개를 정해진 순서로 본다")
	void adminSeesMenusInOrder() {
		MvcTestResult result = asAdmin(mvc.get().uri(MY_MENUS));

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$.length()").isEqualTo(1);
		assertThat(result).bodyJson().extractingPath("$[0].name").isEqualTo("관리");
		assertThat(result).bodyJson().extractingPath("$[0].children[*].name").asArray()
			.containsExactly("사용자 관리", "메뉴 관리", "코드 관리", "접속 이력", "역할 관리");
	}

	@Test
	@DisplayName("US1-AC2 관리자는 관리 화면 5개의 목록을 연다")
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

	private List<String> insertStatementsOf(String script) throws IOException {
		String sql = new ClassPathResource(script).getContentAsString(StandardCharsets.UTF_8)
			.replaceAll("(?m)^\\s*--.*$", "");
		return Arrays.stream(sql.split(";"))
			.map(String::strip)
			.filter(statement -> statement.startsWith("INSERT"))
			.toList();
	}

	private MvcTestResult asAdmin(MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(2L, "ADMIN")).exchange();
	}

	private MvcTestResult asUser(MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(3L, "USER")).exchange();
	}
}
