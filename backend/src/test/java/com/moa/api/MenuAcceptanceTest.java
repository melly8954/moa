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
 * 메뉴 권한 (선택 헬퍼 menu)
 */
class MenuAcceptanceTest extends AcceptanceTest {

	private static final String ADMIN_MENUS = "/api/v1/admin/menus";
	private static final String MY_MENUS = "/api/v1/menus/me";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/** 마이그레이션 시드(관리 > 메뉴 관리)를 비우고 테스트마다 직접 만든다 */
	@BeforeEach
	void clearMenus() {
		jdbcTemplate.update("delete from menus");
	}

	@Test
	@DisplayName("메뉴: 내 메뉴는 권한으로 거르고(권한 키가 없으면 모두, ADMIN은 전부), 보이는 하위 메뉴가 없는 그룹은 뺀다")
	void myMenusByPermission() {
		long group = create(null, "업무", null, 10, null);
		create(group, "주문", "/orders", 10, null);
		create(group, "정산", "/settlements", 20, "MENU_MANAGE");
		create(null, "공지", "/notices", 20, null);
		create(null, "관리", null, 30, "MENU_MANAGE");

		MvcTestResult user = asUser(mvc.get().uri(MY_MENUS));
		MvcTestResult admin = asAdmin(mvc.get().uri(MY_MENUS));

		assertThat(user).hasStatus(200);
		assertThat(user).bodyJson().extractingPath("$.length()").isEqualTo(2);
		assertThat(user).bodyJson().extractingPath("$[0].children.length()").isEqualTo(1);
		assertThat(user).bodyJson().extractingPath("$[0].children[0].path").isEqualTo("/orders");
		assertThat(user).bodyJson().extractingPath("$[1].path").isEqualTo("/notices");
		assertThat(admin).bodyJson().extractingPath("$.length()").isEqualTo(2);
		assertThat(admin).bodyJson().extractingPath("$[0].children.length()").isEqualTo(2);
	}

	@Test
	@DisplayName("메뉴: 사용 안 함으로 바꾸면 내 메뉴에서 빠지고, 관리 트리에는 남는다")
	void disabledHidden() {
		long group = create(null, "업무", null, 10, null);
		long orders = create(group, "주문", "/orders", 10, null);

		assertThat(asAdmin(mvc.put().uri(ADMIN_MENUS + "/" + orders).contentType(MediaType.APPLICATION_JSON)
			.content("{\"parentId\":" + group + ",\"name\":\"주문\",\"path\":\"/orders\",\"sortOrder\":10,"
				+ "\"enabled\":false}")))
			.hasStatus(200);

		assertThat(asUser(mvc.get().uri(MY_MENUS))).bodyJson().extractingPath("$.length()").isEqualTo(0);
		assertThat(asAdmin(mvc.get().uri(ADMIN_MENUS))).bodyJson().extractingPath("$[0].children[0].enabled")
			.isEqualTo(false);
	}

	@Test
	@DisplayName("메뉴: 경로는 /로 시작하는 내부 경로(V001)이고 겹치지 않는다(R002). 권한은 등록된 키만(V001)")
	void pathRules() {
		create(null, "주문", "/orders", 10, null);

		assertThat(post(null, "a", "orders")).hasStatus(400).bodyJson().extractingPath("$.code").isEqualTo("V001");
		assertThat(post(null, "b", "https://evil.example.com")).bodyJson().extractingPath("$.code").isEqualTo("V001");
		assertThat(post(null, "c", "//evil.example.com")).bodyJson().extractingPath("$.code").isEqualTo("V001");
		assertThat(post(null, "d", "/orders")).hasStatus(409).bodyJson().extractingPath("$.code").isEqualTo("R002");
		assertThat(asAdmin(mvc.post().uri(ADMIN_MENUS).contentType(MediaType.APPLICATION_JSON)
			.content(json(null, "e", "/e", 10, "NO_SUCH_PERMISSION"))))
			.hasStatus(400).bodyJson().extractingPath("$.code").isEqualTo("V001");
	}

	@Test
	@DisplayName("메뉴: 2단계까지만 둔다. 3단계가 되는 부모, 자기 자신을 부모로, 하위가 있는 메뉴 지우기는 B001")
	void depthAndDelete() {
		long group = create(null, "업무", null, 10, null);
		long orders = create(group, "주문", "/orders", 10, null);

		assertThat(post(orders, "주문 상세", "/orders/detail")).hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("B001");
		assertThat(asAdmin(mvc.put().uri(ADMIN_MENUS + "/" + group).contentType(MediaType.APPLICATION_JSON)
			.content("{\"parentId\":" + group + ",\"name\":\"업무\",\"sortOrder\":10,\"enabled\":true}")))
			.bodyJson().extractingPath("$.code").isEqualTo("B001");
		assertThat(asAdmin(mvc.delete().uri(ADMIN_MENUS + "/" + group))).hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("B001");
		assertThat(asAdmin(mvc.delete().uri(ADMIN_MENUS + "/" + orders))).hasStatus(204);
		assertThat(asAdmin(mvc.get().uri(ADMIN_MENUS))).bodyJson().extractingPath("$[0].children.length()")
			.isEqualTo(0);
	}

	@Test
	@DisplayName("메뉴: 순서를 한 번에 바꾸고, 하나라도 틀리면 아무것도 바꾸지 않는다")
	void reorder() {
		long group = create(null, "업무", null, 10, null);
		long orders = create(group, "주문", "/orders", 10, null);
		long returns = create(group, "반품", "/returns", 20, null);

		assertThat(reorder("[{\"id\":" + orders + ",\"parentId\":" + group + ",\"sortOrder\":20},"
			+ "{\"id\":" + returns + ",\"parentId\":" + group + ",\"sortOrder\":10}]")).hasStatus(204);
		assertThat(asAdmin(mvc.get().uri(ADMIN_MENUS))).bodyJson().extractingPath("$[0].children[0].path")
			.isEqualTo("/returns");

		assertThat(reorder("[{\"id\":" + orders + ",\"parentId\":" + group + ",\"sortOrder\":5},"
			+ "{\"id\":" + group + ",\"parentId\":" + returns + ",\"sortOrder\":10}]")).hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("B001");
		assertThat(asAdmin(mvc.get().uri(ADMIN_MENUS))).bodyJson().extractingPath("$[0].children[0].path")
			.isEqualTo("/returns");
	}

	@Test
	@DisplayName("메뉴: 관리 API는 MENU_MANAGE 권한(A005), 내 메뉴는 로그인 필요(A001)")
	void access() {
		assertThat(asUser(mvc.get().uri(ADMIN_MENUS))).hasStatus(403)
			.bodyJson().extractingPath("$.code").isEqualTo("A005");
		assertThat(mvc.get().uri(MY_MENUS)).hasStatus(401).bodyJson().extractingPath("$.code").isEqualTo("A001");
	}

	private long create(Long parentId, String name, String path, int sortOrder, String permission) {
		MvcTestResult result = asAdmin(mvc.post().uri(ADMIN_MENUS).contentType(MediaType.APPLICATION_JSON)
			.content(json(parentId, name, path, sortOrder, permission)));
		assertThat(result).hasStatus(201);
		return ((Number)JsonPath.read(body(result), "$.id")).longValue();
	}

	private MvcTestResult post(Long parentId, String name, String path) {
		return asAdmin(mvc.post().uri(ADMIN_MENUS).contentType(MediaType.APPLICATION_JSON)
			.content(json(parentId, name, path, 10, null)));
	}

	private MvcTestResult reorder(String json) {
		return asAdmin(mvc.put().uri(ADMIN_MENUS + "/order").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private String json(Long parentId, String name, String path, int sortOrder, String permission) {
		return "{\"parentId\":" + parentId + ",\"name\":\"" + name + "\",\"path\":" + quote(path)
			+ ",\"sortOrder\":" + sortOrder + ",\"permission\":" + quote(permission) + "}";
	}

	private String quote(String value) {
		return value == null ? "null" : "\"" + value + "\"";
	}

	private MvcTestResult asAdmin(MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(2L, "ADMIN")).exchange();
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
