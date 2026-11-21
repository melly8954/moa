package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;
import com.moa.support.AcceptanceTest;

/**
 * 메뉴 권한 (선택 헬퍼 menu).
 * moa: 메뉴는 경로·권한 대신 등록된 연결 화면을 가리킨다 (specs/001-admin-menus FR-008·FR-009).
 * 경로 형식·겹침 검사는 없어지고, 연결 화면·공개 범위 검사가 그 자리에 온다
 */
class MenuAcceptanceTest extends AcceptanceTest {

	private static final String ADMIN_MENUS = "/api/v1/admin/menus";
	private static final String MY_MENUS = "/api/v1/menus/me";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/** 마이그레이션 시드(메뉴 관리)를 비우고 테스트마다 직접 만든다 */
	@BeforeEach
	void clearMenus() {
		jdbcTemplate.update("delete from tb_menu");
	}

	@Test
	// moa: 관리 메뉴는 가리키는 연결 화면의 권한으로 거른다 (specs/001-admin-menus FR-002). 키트는 메뉴의 권한 키다
	// moa (2026-11-21): 키트의 "ADMIN은 전부"는 "최고 관리자는 전부, 역할 없는 관리자 계정은 없음"으로 바뀌었다 (specs/003-admin-roles R21)
	// moa (2026-11-22): 권한 없는 메뉴도 내려주고 accessible로 알린다. 회원에게는 관리 메뉴가 없다 (001 FR-002)
	@DisplayName("메뉴: 관리자 계정은 관리 메뉴를 모두 받고 연결 화면 권한으로 accessible을 정하며(최고 관리자는 전부, 역할 없으면 없음), 빈 묶음은 뺀다")
	void myMenusByPermission() {
		long group = create(null, "업무", null, 10);
		create(group, "코드", "ADMIN_CODES", 10);
		create(group, "메뉴", "ADMIN_MENUS", 20);
		create(null, "공지", "ADMIN_NOTICES", 20);
		create(null, "빈 묶음", null, 30);

		MvcTestResult user = asUser(mvc.get().uri(MY_MENUS));
		MvcTestResult noRole = mvc.get().uri(MY_MENUS).header(HttpHeaders.AUTHORIZATION, bearer(4L, "ADMIN"))
			.exchange();
		MvcTestResult admin = asAdmin(mvc.get().uri(MY_MENUS));

		assertThat(user).hasStatus(200);
		assertThat(user).bodyJson().extractingPath("$.length()").isEqualTo(0);
		assertThat(noRole).bodyJson().extractingPath("$.length()").isEqualTo(2);
		assertThat(noRole).bodyJson().extractingPath("$..[?(@.accessible == true)].name").asArray().isEmpty();
		assertThat(admin).bodyJson().extractingPath("$.length()").isEqualTo(2);
		assertThat(admin).bodyJson().extractingPath("$[0].children.length()").isEqualTo(2);
		assertThat(admin).bodyJson().extractingPath("$..[?(@.accessible == false)].name").asArray().isEmpty();
	}

	@Test
	@DisplayName("메뉴: 사용 안 함으로 바꾸면 내 메뉴에서 빠지고, 관리 트리에는 남는다")
	void disabledHidden() {
		long group = create(null, "업무", null, 10);
		long codes = create(group, "코드", "ADMIN_CODES", 10);

		assertThat(asAdmin(mvc.put().uri(ADMIN_MENUS + "/" + codes).contentType(MediaType.APPLICATION_JSON)
			.content("{\"parentId\":" + group + ",\"name\":\"코드\",\"screenKey\":\"ADMIN_CODES\",\"sortOrder\":10,"
				+ "\"enabled\":false}")))
			.hasStatus(200);

		assertThat(asAdmin(mvc.get().uri(MY_MENUS))).bodyJson().extractingPath("$.length()").isEqualTo(0);
		assertThat(asAdmin(mvc.get().uri(ADMIN_MENUS))).bodyJson().extractingPath("$[0].children[0].enabled")
			.isEqualTo(false);
	}

	@Test
	// moa: 키트의 경로 규칙(V001·R002)은 연결 화면 규칙으로 바뀌었다 (specs/001-admin-menus FR-009)
	@DisplayName("메뉴: 연결 화면은 등록된 키만(V001), 하위 메뉴는 연결 화면이 있어야 하고(V001), 사용자 화면 메뉴는 공개 범위가 있어야 한다(V001)")
	void screenRules() {
		long group = create(null, "업무", null, 10);

		assertThat(post(json(null, "a", "NO_SUCH_SCREEN", 10, null, null))).hasStatus(400)
			.bodyJson().extractingPath("$.code").isEqualTo("V001");
		assertThat(post(json(null, "b", "MISSING", 10, null, null))).hasStatus(400)
			.bodyJson().extractingPath("$.code").isEqualTo("V001");
		assertThat(post(json(group, "c", null, 10, null, null))).hasStatus(400)
			.bodyJson().extractingPath("$.code").isEqualTo("V001");
		assertThat(post(json(null, "d", "NOTICES", 10, "USER", null))).hasStatus(400)
			.bodyJson().extractingPath("$.code").isEqualTo("V001");
		assertThat(post(json(null, "e", "ADMIN_CODES", 10, null, null))).hasStatus(201);
		assertThat(post(json(null, "f", "ADMIN_CODES", 20, null, null))).as("같은 연결 화면을 여러 메뉴에 건다")
			.hasStatus(201);
	}

	@Test
	@DisplayName("메뉴: 2단계까지만 둔다. 3단계가 되는 부모, 자기 자신을 부모로, 하위가 있는 메뉴 지우기는 B001")
	void depthAndDelete() {
		long group = create(null, "업무", null, 10);
		long codes = create(group, "코드", "ADMIN_CODES", 10);

		assertThat(post(json(codes, "코드 상세", "ADMIN_MENUS", 10, null, null))).hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("B001");
		assertThat(asAdmin(mvc.put().uri(ADMIN_MENUS + "/" + group).contentType(MediaType.APPLICATION_JSON)
			.content("{\"parentId\":" + group + ",\"name\":\"업무\",\"sortOrder\":10,\"enabled\":true}")))
			.bodyJson().extractingPath("$.code").isEqualTo("B001");
		assertThat(asAdmin(mvc.delete().uri(ADMIN_MENUS + "/" + group))).hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("B001");
		assertThat(asAdmin(mvc.delete().uri(ADMIN_MENUS + "/" + codes))).hasStatus(204);
		assertThat(asAdmin(mvc.get().uri(ADMIN_MENUS))).bodyJson().extractingPath("$[0].children.length()")
			.isEqualTo(0);
	}

	@Test
	@DisplayName("메뉴: 순서를 한 번에 바꾸고, 하나라도 틀리면 아무것도 바꾸지 않는다")
	void reorder() {
		long group = create(null, "업무", null, 10);
		long codes = create(group, "코드", "ADMIN_CODES", 10);
		long menus = create(group, "메뉴", "ADMIN_MENUS", 20);

		assertThat(reorder("[{\"id\":" + codes + ",\"parentId\":" + group + ",\"sortOrder\":20},"
			+ "{\"id\":" + menus + ",\"parentId\":" + group + ",\"sortOrder\":10}]")).hasStatus(204);
		assertThat(asAdmin(mvc.get().uri(ADMIN_MENUS))).bodyJson().extractingPath("$[0].children[0].name")
			.isEqualTo("메뉴");

		assertThat(reorder("[{\"id\":" + codes + ",\"parentId\":" + group + ",\"sortOrder\":5},"
			+ "{\"id\":" + group + ",\"parentId\":" + menus + ",\"sortOrder\":10}]")).hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("B001");
		assertThat(asAdmin(mvc.get().uri(ADMIN_MENUS))).bodyJson().extractingPath("$[0].children[0].name")
			.isEqualTo("메뉴");
	}

	@Test
	// moa: 메뉴 대상은 연결 화면의 대상이라 묶음 메뉴를 하위로, 다른 대상의 부모 아래로 옮기지 않는다 (specs/001-admin-menus FR-009)
	@DisplayName("메뉴: 묶음 메뉴를 하위로 옮기거나 대상이 다른 부모 아래로 옮기면 B001")
	void reorderKeepsScreenRules() {
		long group = create(null, "업무", null, 10);
		long other = create(null, "다른 묶음", null, 20);
		long notices = ((Number)JsonPath.read(body(post(json(null, "공지", "NOTICES", 30, "USER", "PUBLIC"))), "$.id"))
			.longValue();

		assertThat(reorder("[{\"id\":" + other + ",\"parentId\":" + group + ",\"sortOrder\":10}]")).hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("B001");
		assertThat(reorder("[{\"id\":" + notices + ",\"parentId\":" + group + ",\"sortOrder\":10}]")).hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("B001");
	}

	@Test
	// moa: 일괄 처리 (specs/001-admin-menus FR-010)
	@DisplayName("메뉴: 일괄 사용 여부·지우기는 없는 메뉴가 섞이면 R001로 아무것도 바꾸지 않고, 묶음은 하위 메뉴와 함께 지울 수 있다")
	void bulkRules() {
		long group = create(null, "업무", null, 10);
		long codes = create(group, "코드", "ADMIN_CODES", 10);

		assertThat(asAdmin(mvc.put().uri(ADMIN_MENUS + "/enabled").contentType(MediaType.APPLICATION_JSON)
			.content("{\"ids\":[" + codes + ",999999],\"enabled\":false}")))
			.hasStatus(404).bodyJson().extractingPath("$.code").isEqualTo("R001");
		assertThat(asAdmin(mvc.get().uri(ADMIN_MENUS))).bodyJson().extractingPath("$[0].children[0].enabled")
			.isEqualTo(true);

		assertThat(asAdmin(mvc.post().uri(ADMIN_MENUS + "/bulk-delete").contentType(MediaType.APPLICATION_JSON)
			.content("{\"ids\":[" + group + "," + codes + "]}")))
			.hasStatus(204);
		assertThat(asAdmin(mvc.get().uri(ADMIN_MENUS))).bodyJson().extractingPath("$.length()").isEqualTo(0);
	}

	@Test
	@DisplayName("메뉴: 관리 API는 MENU_MANAGE 권한(A005), 내 메뉴는 로그인 필요(A001)")
	void access() {
		assertThat(asUser(mvc.get().uri(ADMIN_MENUS))).hasStatus(403)
			.bodyJson().extractingPath("$.code").isEqualTo("A005");
		assertThat(mvc.get().uri(MY_MENUS)).hasStatus(401).bodyJson().extractingPath("$.code").isEqualTo("A001");
	}

	private long create(Long parentId, String name, String screenKey, int sortOrder) {
		MvcTestResult result = post(json(parentId, name, screenKey, sortOrder, null, null));
		assertThat(result).hasStatus(201);
		return ((Number)JsonPath.read(body(result), "$.id")).longValue();
	}

	private MvcTestResult post(String json) {
		return asAdmin(mvc.post().uri(ADMIN_MENUS).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private MvcTestResult reorder(String json) {
		return asAdmin(mvc.put().uri(ADMIN_MENUS + "/order").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private String json(Long parentId, String name, String screenKey, int sortOrder, String audience,
		String visibility) {
		return "{\"parentId\":" + parentId + ",\"name\":\"" + name + "\",\"screenKey\":" + quote(screenKey)
			+ ",\"sortOrder\":" + sortOrder + ",\"audience\":" + quote(audience) + ",\"visibility\":"
			+ quote(visibility) + "}";
	}

	private String quote(String value) {
		return value == null ? "null" : "\"" + value + "\"";
	}

	private MvcTestResult asAdmin(MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, superAdmin()).exchange();
	}

	private MvcTestResult asUser(MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(3L, "USER")).exchange();
	}

	private String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString();
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
