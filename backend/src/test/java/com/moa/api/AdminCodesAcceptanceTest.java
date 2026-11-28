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
 * 그룹은 DB에 두고 관리자가 다룬다. 그룹은 지우지 않고 사용 여부로 끄며, 항목은 사용 안 함일 때만 지운다 (FR-008).
 * 각 테스트는 그룹 둘을 API로 만들고 시작한다 (research R13). US1-AC6은 그룹을 만들지 않은 상태를 따로 본다.
 * US1-AC10(저장하지 않은 변경 묻기)은 화면 동작이라 e2e가 본다
 */
class AdminCodesAcceptanceTest extends AcceptanceTest {

	private static final String GENRE_GROUP = "TEST_GENRE";
	private static final String REASON_GROUP = "TEST_REASON";
	private static final String GROUPS = "/api/v1/admin/code-groups";
	private static final String GENRE = GROUPS + "/" + GENRE_GROUP + "/codes";
	private static final String REASON = GROUPS + "/" + REASON_GROUP + "/codes";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void createGroups() {
		// 마이그레이션이 넣은 그룹(PLAY_TIME·SUPPORT_CATEGORY 등, specs/004-admin-codes FR-001)은 DatabaseCleaner가 첫 테스트 뒤에야 지운다.
		// 이 클래스가 먼저 돌아도 같게 시험 그룹만 남긴다
		jdbcTemplate.update("DELETE FROM tb_code");
		jdbcTemplate.update("DELETE FROM tb_code_group");
		assertThat(post(GROUPS, "{\"groupCode\":\"" + GENRE_GROUP + "\",\"name\":\"시험 장르\",\"description\":\"게임 장르\"}"))
			.hasStatus(201);
		assertThat(post(GROUPS, "{\"groupCode\":\"" + REASON_GROUP + "\",\"name\":\"시험 사유\"}")).hasStatus(201);
	}

	@Test
	@DisplayName("US1-AC1 공통코드 트리는 그룹 순서(처음은 만든 순서)로 그룹마다 이름·설명·그룹 코드·사용 여부와 항목을 준다")
	void treeOfGroups() {
		item(GENRE, "FPS", "FPS");

		MvcTestResult tree = admin(mvc.get().uri(GROUPS + "/tree"));

		assertThat(tree).hasStatus(200);
		assertThat(tree).bodyJson().extractingPath("$[*].groupCode").asArray()
			.containsExactly(GENRE_GROUP, REASON_GROUP);
		assertThat(tree).bodyJson().extractingPath("$[0].name").isEqualTo("시험 장르");
		assertThat(tree).bodyJson().extractingPath("$[0].description").isEqualTo("게임 장르");
		assertThat(tree).bodyJson().extractingPath("$[0].enabled").isEqualTo(true);
		assertThat(tree).bodyJson().extractingPath("$[0].codes[*].name").asArray().containsExactly("FPS");
		assertThat(tree).bodyJson().extractingPath("$[1].codes").asArray().isEmpty();
		assertThat(tree).bodyJson().doesNotHavePath("$[0].usedBy");
		assertThat(tree).bodyJson().doesNotHavePath("$[0].system");
	}

	@Test
	@DisplayName("US1-AC2 코드와 이름을 적어 만들면 그 그룹 맨 뒤에 그 코드로 생긴다. 코드가 없거나 비면 V001(자동 순번 없음), 겹치면 R002")
	void createWithCode() {
		assertThat(item(GENRE, "FPS", "FPS")).hasStatus(201).bodyJson().extractingPath("$.code").isEqualTo("FPS");
		assertThat(item(GENRE, "RPG", "롤플레잉")).bodyJson().extractingPath("$.code").isEqualTo("RPG");

		assertThat(admin(mvc.get().uri(GENRE))).bodyJson().extractingPath("$[*].code").asArray()
			.containsExactly("FPS", "RPG");
		assertThat(post(GENRE, "{\"name\":\"코드 없음\"}")).hasStatus(400).bodyJson().extractingPath("$.code")
			.isEqualTo("V001");
		assertThat(item(GENRE, "", "빈 코드")).hasStatus(400).bodyJson().extractingPath("$.code").isEqualTo("V001");
		assertThat(item(GENRE, "fps", "소문자")).hasStatus(400).bodyJson().extractingPath("$.code").isEqualTo("V001");
		assertThat(item(GENRE, "FPS", "겹침")).hasStatus(409).bodyJson().extractingPath("$.code").isEqualTo("R002");
		// 다른 그룹에서는 같은 코드를 쓸 수 있다
		assertThat(item(REASON, "FPS", "다른 그룹")).hasStatus(201);
	}

	@Test
	@DisplayName("US1-AC3 항목은 이름과 사용 여부를 고치고 코드는 그대로다")
	void modifyNameAndEnabled() {
		item(GENRE, "FPS", "FPS");

		MvcTestResult modified = put(GENRE + "/FPS", "{\"name\":\"1인칭 슈팅\",\"enabled\":false}");

		assertThat(modified).hasStatus(200);
		assertThat(modified).bodyJson().extractingPath("$.code").isEqualTo("FPS");
		assertThat(modified).bodyJson().extractingPath("$.name").isEqualTo("1인칭 슈팅");
		assertThat(modified).bodyJson().extractingPath("$.enabled").isEqualTo(false);
		assertThat(modified).bodyJson().extractingPath("$.sortOrder").isEqualTo(0);
	}

	@Test
	@DisplayName("US1-AC4 한 그룹 안의 항목 둘을 한꺼번에 사용 안 함으로 바꾸면 화면용 목록에서 빠진다. 없는 코드가 섞이면 아무것도 바꾸지 않는다")
	void bulkDisable() {
		item(GENRE, "FPS", "FPS");
		item(GENRE, "RPG", "롤플레잉");
		item(GENRE, "MOBA", "MOBA");
		item(REASON, "ABUSE", "욕설");
		String bulk = GENRE + "/enabled";

		assertThat(put(bulk, "{\"codes\":[\"FPS\",\"ABUSE\"],\"enabled\":false}")).hasStatus(404).bodyJson()
			.extractingPath("$.code").isEqualTo("R001");
		assertThat(screenCodes(GENRE_GROUP)).bodyJson().extractingPath("$.length()").isEqualTo(3);

		assertThat(put(bulk, "{\"codes\":[\"FPS\",\"RPG\"],\"enabled\":false}")).hasStatus(204);

		assertThat(screenCodes(GENRE_GROUP)).bodyJson().extractingPath("$[*].code").asArray().containsExactly("MOBA");
		assertThat(screenCodes(REASON_GROUP)).bodyJson().extractingPath("$[*].code").asArray()
			.containsExactly("ABUSE");
	}

	@Test
	@DisplayName("US1-AC5 같은 그룹 안에서 순서를 바꾸면 화면용 목록도 그 순서다. 다른 그룹의 코드가 섞이면 아무것도 바꾸지 않는다")
	void reorderInGroup() {
		item(GENRE, "FPS", "FPS");
		item(GENRE, "RPG", "롤플레잉");
		item(REASON, "SPAM", "도배");

		assertThat(put(GENRE + "/order", "[{\"code\":\"RPG\",\"sortOrder\":0},{\"code\":\"FPS\",\"sortOrder\":1}]"))
			.hasStatus(204);
		assertThat(screenCodes(GENRE_GROUP)).bodyJson().extractingPath("$[*].code").asArray()
			.containsExactly("RPG", "FPS");

		// REASON에만 있는 코드는 GENRE에서 찾지 못한다
		assertThat(put(GENRE + "/order", "[{\"code\":\"FPS\",\"sortOrder\":0},{\"code\":\"SPAM\",\"sortOrder\":1}]"))
			.hasStatus(404).bodyJson().extractingPath("$.code").isEqualTo("R001");
		assertThat(screenCodes(GENRE_GROUP)).bodyJson().extractingPath("$[*].code").asArray()
			.containsExactly("RPG", "FPS");
	}

	@Test
	@DisplayName("US1-AC5 그룹 순서를 바꾸면 트리가 그 순서다. 없는 그룹이 섞이면 아무것도 바꾸지 않는다")
	void reorderGroups() {
		String order = GROUPS + "/order";

		assertThat(put(order, "[{\"groupCode\":\"" + REASON_GROUP + "\",\"sortOrder\":0},{\"groupCode\":\""
			+ GENRE_GROUP + "\",\"sortOrder\":1}]")).hasStatus(204);
		assertThat(admin(mvc.get().uri(GROUPS + "/tree"))).bodyJson().extractingPath("$[*].groupCode").asArray()
			.containsExactly(REASON_GROUP, GENRE_GROUP);

		assertThat(put(order, "[{\"groupCode\":\"" + GENRE_GROUP + "\",\"sortOrder\":0},{\"groupCode\":\"NONE\","
			+ "\"sortOrder\":1}]")).hasStatus(404).bodyJson().extractingPath("$.code").isEqualTo("R001");
		assertThat(admin(mvc.get().uri(GROUPS + "/tree"))).bodyJson().extractingPath("$[*].groupCode").asArray()
			.containsExactly(REASON_GROUP, GENRE_GROUP);
	}

	@Test
	@DisplayName("US1-AC6 그룹이 하나도 없으면 트리는 빈 목록이다")
	void emptyTree() {
		// @BeforeEach가 만든 그룹은 API로 지울 수 없으므로(FR-008) 표를 직접 비워 처음 상태를 만든다
		jdbcTemplate.update("DELETE FROM tb_code_group");

		assertThat(admin(mvc.get().uri(GROUPS + "/tree"))).hasStatus(200).bodyJson().extractingPath("$.length()")
			.isEqualTo(0);
	}

	@Test
	@DisplayName("US1-AC7 관리자가 만든 그룹은 사용 중으로 맨 뒤에 생기고, 겹친 그룹 코드는 R002, 형식이 틀리면 V001")
	void createGroup() {
		MvcTestResult created = post(GROUPS, "{\"groupCode\":\"EVENT_TYPE\",\"name\":\"이벤트 분류\"}");

		assertThat(created).hasStatus(201);
		assertThat(created).bodyJson().extractingPath("$.enabled").isEqualTo(true);
		assertThat(admin(mvc.get().uri(GROUPS + "/tree"))).bodyJson().extractingPath("$[*].groupCode").asArray()
			.containsExactly(GENRE_GROUP, REASON_GROUP, "EVENT_TYPE");
		assertThat(post(GROUPS, "{\"groupCode\":\"" + GENRE_GROUP + "\",\"name\":\"겹침\"}")).hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("R002");
		assertThat(post(GROUPS, "{\"groupCode\":\"1_BAD\",\"name\":\"틀림\"}")).hasStatus(400).bodyJson()
			.extractingPath("$.code").isEqualTo("V001");
	}

	@Test
	@DisplayName("US1-AC8 그룹은 이름·설명·사용 여부를 고치고 그룹 코드는 그대로다. 그룹을 지우는 API는 없다")
	void modifyGroup() {
		item(GENRE, "FPS", "FPS");

		MvcTestResult modified = put(GROUPS + "/" + GENRE_GROUP,
			"{\"name\":\"게임 장르\",\"description\":\"장르\",\"enabled\":true}");

		assertThat(modified).hasStatus(200);
		assertThat(modified).bodyJson().extractingPath("$.groupCode").isEqualTo(GENRE_GROUP);
		assertThat(modified).bodyJson().extractingPath("$.name").isEqualTo("게임 장르");
		assertThat(modified).bodyJson().extractingPath("$.description").isEqualTo("장르");
		assertThat(admin(mvc.delete().uri(GROUPS + "/" + GENRE_GROUP))).hasStatus(405);
	}

	@Test
	@DisplayName("US1-AC9 그룹을 끄면 화면용 목록은 빈 목록이고 항목은 남는다. includeDisabled면 항목을 주고, 다시 켜면 보인다")
	void disableGroup() {
		item(REASON, "ABUSE", "욕설");
		item(REASON, "AD", "광고");

		assertThat(put(GROUPS + "/" + REASON_GROUP, "{\"name\":\"시험 사유\",\"enabled\":false}")).hasStatus(200)
			.bodyJson().extractingPath("$.enabled").isEqualTo(false);

		assertThat(screenCodes(REASON_GROUP)).hasStatus(200).bodyJson().extractingPath("$.length()").isEqualTo(0);
		assertThat(mvc.get().uri("/api/v1/codes/" + REASON_GROUP + "?includeDisabled=true")
			.header(HttpHeaders.AUTHORIZATION, bearer(3L, "USER"))).bodyJson().extractingPath("$.length()")
			.isEqualTo(2);
		assertThat(admin(mvc.get().uri(GROUPS + "/tree"))).bodyJson().extractingPath("$[1].codes.length()")
			.isEqualTo(2);

		put(GROUPS + "/" + REASON_GROUP, "{\"name\":\"시험 사유\",\"enabled\":true}");

		assertThat(screenCodes(REASON_GROUP)).bodyJson().extractingPath("$[*].name").asArray()
			.containsExactly("욕설", "광고");
	}

	@Test
	@DisplayName("US1-AC11 사용 안 함 항목만 지운다. 지우면 관리·화면용 목록에서 빠지고 같은 코드로 다시 만든다. 사용 중이면 B001, 없으면 R001")
	void deleteDisabledCode() {
		item(GENRE, "FPS", "FPS");
		item(GENRE, "TYPO", "오타");

		assertThat(admin(mvc.delete().uri(GENRE + "/TYPO"))).hasStatus(409).bodyJson().extractingPath("$.code")
			.isEqualTo("B001");
		assertThat(admin(mvc.get().uri(GENRE))).bodyJson().extractingPath("$[*].code").asArray()
			.containsExactly("FPS", "TYPO");

		put(GENRE + "/TYPO", "{\"name\":\"오타\",\"enabled\":false}");
		assertThat(admin(mvc.delete().uri(GENRE + "/TYPO"))).hasStatus(204);

		assertThat(admin(mvc.get().uri(GENRE))).bodyJson().extractingPath("$[*].code").asArray()
			.containsExactly("FPS");
		assertThat(mvc.get().uri("/api/v1/codes/" + GENRE_GROUP + "?includeDisabled=true")
			.header(HttpHeaders.AUTHORIZATION, bearer(3L, "USER"))).bodyJson().extractingPath("$[*].code").asArray()
			.containsExactly("FPS");
		// 소프트 삭제라 줄은 남는다
		assertThat(jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM tb_code WHERE group_code = ? AND code = 'TYPO' AND deleted_at IS NOT NULL",
			Integer.class, GENRE_GROUP)).isEqualTo(1);
		assertThat(item(GENRE, "TYPO", "다시")).hasStatus(201);
		assertThat(admin(mvc.delete().uri(GENRE + "/NONE"))).hasStatus(404).bodyJson().extractingPath("$.code")
			.isEqualTo("R001");
	}

	@Test
	@DisplayName("없는 그룹은 관리·화면용 모두 R001")
	void unknownGroup() {
		assertThat(item(GROUPS + "/NONE/codes", "NONE", "없음")).hasStatus(404).bodyJson()
			.extractingPath("$.code").isEqualTo("R001");
		assertThat(mvc.get().uri("/api/v1/codes/NONE").header(HttpHeaders.AUTHORIZATION, bearer(3L, "USER")))
			.hasStatus(404);
	}

	private MvcTestResult screenCodes(String groupCode) {
		return mvc.get().uri("/api/v1/codes/" + groupCode)
			.header(HttpHeaders.AUTHORIZATION, bearer(3L, "USER")).exchange();
	}

	private MvcTestResult item(String uri, String code, String name) {
		return post(uri, "{\"code\":\"" + code + "\",\"name\":\"" + name + "\"}");
	}

	private MvcTestResult post(String uri, String json) {
		return admin(mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private MvcTestResult put(String uri, String json) {
		return admin(mvc.put().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private MvcTestResult admin(MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, superAdmin()).exchange();
	}
}
