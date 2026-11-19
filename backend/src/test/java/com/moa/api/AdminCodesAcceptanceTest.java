package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.moa.support.AcceptanceTest;

/**
 * 공통코드. 스펙 specs/004-admin-codes의 수용 시나리오다.
 * 그룹은 DB에 두고 관리자가 다룬다. 시스템 그룹 하나(JDBC, API로는 만들 수 없다)와 일반 그룹 하나(API)로 시작한다
 * (research R10). US1-AC6은 그 그룹을 지우고 본다
 */
class AdminCodesAcceptanceTest extends AcceptanceTest {

	/** 시스템 그룹: 기능이 마이그레이션으로 넣는 그룹을 흉내 낸다 */
	private static final String GENRE_GROUP = "TEST_GENRE";
	/** 일반 그룹: 관리자가 화면에서 만든다 */
	private static final String REASON_GROUP = "TEST_REASON";
	private static final String GROUPS = "/api/v1/admin/code-groups";
	private static final String GENRE = GROUPS + "/" + GENRE_GROUP + "/codes";
	private static final String REASON = GROUPS + "/" + REASON_GROUP + "/codes";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void createGroups() {
		jdbcTemplate.update("INSERT INTO code_groups (group_code, name, description, used_by, `system`, created_at, "
			+ "updated_at, created_by, updated_by) "
			+ "VALUES (?, '시험 장르', '게임 장르', '시험 게임 화면', TRUE, NOW(6), NOW(6), 1, 1)",
			GENRE_GROUP);
		assertThat(post(GROUPS, "{\"groupCode\":\"" + REASON_GROUP + "\",\"name\":\"시험 사유\"}")).hasStatus(201);
	}

	@Test
	@DisplayName("US1-AC1 공통코드 트리는 만든 순서로 그룹마다 이름·그룹 코드·쓰는 곳·시스템 여부와 항목을 준다")
	void treeOfGroups() {
		post(GENRE, "{\"name\":\"FPS\"}");

		MvcTestResult tree = admin(mvc.get().uri(GROUPS + "/tree"));

		assertThat(tree).hasStatus(200);
		assertThat(tree).bodyJson().extractingPath("$[*].groupCode").asArray()
			.containsExactly(GENRE_GROUP, REASON_GROUP);
		assertThat(tree).bodyJson().extractingPath("$[0].name").isEqualTo("시험 장르");
		assertThat(tree).bodyJson().extractingPath("$[0].usedBy").isEqualTo("시험 게임 화면");
		assertThat(tree).bodyJson().extractingPath("$[0].system").isEqualTo(true);
		assertThat(tree).bodyJson().extractingPath("$[1].system").isEqualTo(false);
		assertThat(tree).bodyJson().extractingPath("$[0].codes[*].name").asArray().containsExactly("FPS");
		assertThat(tree).bodyJson().extractingPath("$[1].codes").asArray().isEmpty();
	}

	@Test
	@DisplayName("US1-AC2 이름만 적고 만들면 그 그룹 맨 뒤에 생기고 코드는 겹치지 않는 순번이다 (지운 순번은 다시 쓰지 않는다)")
	void autoNumbering() {
		assertThat(post(GENRE, "{\"name\":\"FPS\"}")).hasStatus(201).bodyJson().extractingPath("$.code")
			.isEqualTo("001");
		assertThat(post(GENRE, "{\"name\":\"RPG\"}")).bodyJson().extractingPath("$.code").isEqualTo("002");
		assertThat(post(REASON, "{\"name\":\"욕설\"}")).bodyJson().extractingPath("$.code").isEqualTo("001");
		assertThat(admin(mvc.delete().uri(GENRE + "/002"))).hasStatus(204);

		MvcTestResult again = post(GENRE, "{\"name\":\"퍼즐\"}");

		assertThat(again).hasStatus(201).bodyJson().extractingPath("$.code").isEqualTo("003");
		assertThat(admin(mvc.get().uri(GENRE))).bodyJson().extractingPath("$[*].name").asArray()
			.containsExactly("FPS", "퍼즐");
		assertThat(post(GENRE, "{\"code\":\"001\",\"name\":\"겹침\"}")).hasStatus(409).bodyJson()
			.extractingPath("$.code").isEqualTo("R002");
	}

	@Test
	@DisplayName("US1-AC3 항목은 이름과 사용 여부를 고치고 코드는 그대로다")
	void modifyNameAndEnabled() {
		post(GENRE, "{\"name\":\"FPS\"}");

		MvcTestResult modified = admin(mvc.put().uri(GENRE + "/001").contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"1인칭 슈팅\",\"enabled\":false}"));

		assertThat(modified).hasStatus(200);
		assertThat(modified).bodyJson().extractingPath("$.code").isEqualTo("001");
		assertThat(modified).bodyJson().extractingPath("$.name").isEqualTo("1인칭 슈팅");
		assertThat(modified).bodyJson().extractingPath("$.enabled").isEqualTo(false);
		assertThat(modified).bodyJson().extractingPath("$.sortOrder").isEqualTo(0);
	}

	@Test
	@DisplayName("US1-AC4 항목 둘을 한꺼번에 사용 안 함으로 바꾸면 화면용 목록에서 빠진다. 하나라도 없으면 아무것도 바꾸지 않는다")
	void bulkDisable() {
		post(GENRE, "{\"name\":\"FPS\"}");
		post(GENRE, "{\"name\":\"RPG\"}");
		post(GENRE, "{\"name\":\"퍼즐\"}");

		assertThat(put(GENRE + "/enabled", "{\"codes\":[\"001\",\"NONE\"],\"enabled\":false}")).hasStatus(404)
			.bodyJson().extractingPath("$.code").isEqualTo("R001");
		assertThat(screenCodes()).bodyJson().extractingPath("$.length()").isEqualTo(3);

		assertThat(put(GENRE + "/enabled", "{\"codes\":[\"001\",\"002\"],\"enabled\":false}")).hasStatus(204);

		assertThat(screenCodes()).bodyJson().extractingPath("$[*].name").asArray().containsExactly("퍼즐");
		assertThat(admin(mvc.post().uri(GENRE + "/bulk-delete").contentType(MediaType.APPLICATION_JSON)
			.content("{\"codes\":[\"001\",\"003\"]}"))).hasStatus(204);
		assertThat(admin(mvc.get().uri(GENRE))).bodyJson().extractingPath("$[*].code").asArray()
			.containsExactly("002");
	}

	@Test
	@DisplayName("US1-AC5 같은 그룹 안에서 순서를 바꾸면 화면용 목록도 그 순서다. 다른 그룹의 코드가 섞이면 아무것도 바꾸지 않는다")
	void reorderInGroup() {
		post(GENRE, "{\"name\":\"FPS\"}");
		post(GENRE, "{\"name\":\"RPG\"}");
		post(REASON, "{\"name\":\"욕설\"}");
		post(REASON, "{\"name\":\"광고\"}");

		assertThat(put(GENRE + "/order", "[{\"code\":\"002\",\"sortOrder\":0},{\"code\":\"001\",\"sortOrder\":1}]"))
			.hasStatus(204);
		assertThat(screenCodes()).bodyJson().extractingPath("$[*].name").asArray().containsExactly("RPG", "FPS");

		// REASON에만 있는 코드는 GENRE에서 찾지 못한다 (GENRE에는 003이 없다)
		post(REASON, "{\"name\":\"도배\"}");
		assertThat(put(GENRE + "/order", "[{\"code\":\"001\",\"sortOrder\":0},{\"code\":\"003\",\"sortOrder\":1}]"))
			.hasStatus(404).bodyJson().extractingPath("$.code").isEqualTo("R001");
		assertThat(screenCodes()).bodyJson().extractingPath("$[*].name").asArray().containsExactly("RPG", "FPS");
	}

	@Test
	@DisplayName("US1-AC6 그룹이 하나도 없으면 트리는 빈 목록이다")
	void emptyTree() {
		jdbcTemplate.update("DELETE FROM code_groups");

		assertThat(admin(mvc.get().uri(GROUPS + "/tree"))).hasStatus(200).bodyJson().extractingPath("$.length()")
			.isEqualTo(0);
	}

	@Test
	@DisplayName("US1-AC7 관리자가 만든 그룹은 일반 그룹으로 맨 뒤에 생기고, 겹친 그룹 코드는 R002, 형식이 틀리면 V001")
	void createGroup() {
		MvcTestResult created = post(GROUPS,
			"{\"groupCode\":\"EVENT_TYPE\",\"name\":\"이벤트 분류\",\"usedBy\":\"이벤트 화면\"}");

		assertThat(created).hasStatus(201);
		assertThat(created).bodyJson().extractingPath("$.system").isEqualTo(false);
		assertThat(admin(mvc.get().uri(GROUPS + "/tree"))).bodyJson().extractingPath("$[*].groupCode").asArray()
			.containsExactly(GENRE_GROUP, REASON_GROUP, "EVENT_TYPE");
		assertThat(post(GROUPS, "{\"groupCode\":\"" + GENRE_GROUP + "\",\"name\":\"겹침\"}")).hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("R002");
		assertThat(post(GROUPS, "{\"groupCode\":\"1_BAD\",\"name\":\"틀림\"}")).hasStatus(400).bodyJson()
			.extractingPath("$.code").isEqualTo("V001");
	}

	@Test
	@DisplayName("US1-AC8 그룹은 이름·설명·쓰는 곳을 고치고 그룹 코드와 시스템 여부는 그대로다. 시스템 그룹도 고칠 수 있다")
	void modifyGroup() {
		MvcTestResult modified = put(GROUPS + "/" + GENRE_GROUP,
			"{\"name\":\"게임 장르\",\"description\":\"장르\",\"usedBy\":\"게임 등록\"}");

		assertThat(modified).hasStatus(200);
		assertThat(modified).bodyJson().extractingPath("$.groupCode").isEqualTo(GENRE_GROUP);
		assertThat(modified).bodyJson().extractingPath("$.name").isEqualTo("게임 장르");
		assertThat(modified).bodyJson().extractingPath("$.usedBy").isEqualTo("게임 등록");
		assertThat(modified).bodyJson().extractingPath("$.system").isEqualTo(true);
	}

	@Test
	@DisplayName("US1-AC9 일반 그룹을 지우면 항목도 함께 지워져 트리와 화면용 목록에서 빠진다")
	void deleteGroup() {
		post(REASON, "{\"name\":\"욕설\"}");
		post(REASON, "{\"name\":\"광고\"}");

		assertThat(admin(mvc.delete().uri(GROUPS + "/" + REASON_GROUP))).hasStatus(204);

		assertThat(admin(mvc.get().uri(GROUPS + "/tree"))).bodyJson().extractingPath("$[*].groupCode").asArray()
			.containsExactly(GENRE_GROUP);
		assertThat(mvc.get().uri("/api/v1/codes/" + REASON_GROUP).header(HttpHeaders.AUTHORIZATION, bearer(3L, "USER")))
			.hasStatus(404).bodyJson().extractingPath("$.code").isEqualTo("R001");
	}

	@Test
	@DisplayName("US1-AC10 시스템 그룹은 지울 수 없다 (B001). 그룹과 항목이 그대로다")
	void systemGroupCannotBeDeleted() {
		post(GENRE, "{\"name\":\"FPS\"}");

		assertThat(admin(mvc.delete().uri(GROUPS + "/" + GENRE_GROUP))).hasStatus(409).bodyJson()
			.extractingPath("$.code").isEqualTo("B001");
		assertThat(screenCodes()).bodyJson().extractingPath("$[*].name").asArray().containsExactly("FPS");
	}

	@Test
	@DisplayName("없는 그룹은 관리·화면용 모두 R001")
	void unknownGroup() {
		assertThat(post(GROUPS + "/NONE/codes", "{\"name\":\"없음\"}")).hasStatus(404).bodyJson()
			.extractingPath("$.code").isEqualTo("R001");
		assertThat(mvc.get().uri("/api/v1/codes/NONE").header(HttpHeaders.AUTHORIZATION, bearer(3L, "USER")))
			.hasStatus(404);
	}

	private MvcTestResult screenCodes() {
		return mvc.get().uri("/api/v1/codes/" + GENRE_GROUP)
			.header(HttpHeaders.AUTHORIZATION, bearer(3L, "USER")).exchange();
	}

	private MvcTestResult post(String uri, String json) {
		return admin(mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private MvcTestResult put(String uri, String json) {
		return admin(mvc.put().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private MvcTestResult admin(MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(2L, "ADMIN")).exchange();
	}
}
