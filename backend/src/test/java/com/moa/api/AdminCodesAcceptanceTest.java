package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.moa.support.AcceptanceTest;
import com.moa.support.TestCodeGroups;

/**
 * 공통코드. 스펙 specs/004-admin-codes의 수용 시나리오다.
 * 그룹은 코드에 등록한다. 테스트에서만 등록하는 그룹 둘(TestCodeGroups)로 본다 (research R8).
 * US1-AC6(등록 그룹 없음)은 CodeGroupRegistryTest가 본다 (테스트 실행에는 시험 그룹이 언제나 등록되어 있다)
 */
class AdminCodesAcceptanceTest extends AcceptanceTest {

	private static final String GROUPS = "/api/v1/admin/code-groups";
	private static final String GENRE = GROUPS + "/" + TestCodeGroups.GENRE + "/codes";
	private static final String REASON = GROUPS + "/" + TestCodeGroups.REASON + "/codes";

	@Test
	@DisplayName("US1-AC1 공통코드 트리는 등록된 그룹마다 이름·그룹 코드·쓰는 곳과 항목을 주고, 그룹 만들기 API는 없다")
	void treeOfRegisteredGroups() {
		post(GENRE, "{\"name\":\"FPS\"}");

		MvcTestResult tree = admin(mvc.get().uri(GROUPS + "/tree"));

		assertThat(tree).hasStatus(200);
		assertThat(tree).bodyJson().extractingPath("$[*].groupCode").asArray()
			.containsExactly(TestCodeGroups.GENRE, TestCodeGroups.REASON);
		assertThat(tree).bodyJson().extractingPath("$[0].name").isEqualTo("시험 장르");
		assertThat(tree).bodyJson().extractingPath("$[0].usedBy").isEqualTo("시험 게임 화면");
		assertThat(tree).bodyJson().extractingPath("$[0].codes[*].name").asArray().containsExactly("FPS");
		assertThat(tree).bodyJson().extractingPath("$[1].codes").asArray().isEmpty();
		assertThat(post(GROUPS, "{\"groupCode\":\"NEW_GROUP\",\"name\":\"새 그룹\"}")).hasStatus(405);
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
	@DisplayName("등록되지 않은 그룹은 관리·화면용 모두 R001")
	void unregisteredGroup() {
		assertThat(post(GROUPS + "/NONE/codes", "{\"name\":\"없음\"}")).hasStatus(404).bodyJson()
			.extractingPath("$.code").isEqualTo("R001");
		assertThat(mvc.get().uri("/api/v1/codes/NONE").header(HttpHeaders.AUTHORIZATION, bearer(3L, "USER")))
			.hasStatus(404);
	}

	private MvcTestResult screenCodes() {
		return mvc.get().uri("/api/v1/codes/" + TestCodeGroups.GENRE)
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
