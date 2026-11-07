package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.moa.support.AcceptanceTest;

/**
 * 공통코드 (선택 헬퍼 code)
 */
class CodeAcceptanceTest extends AcceptanceTest {

	private static final String GROUPS = "/api/v1/admin/code-groups";
	private static final String ADMIN_CODES = GROUPS + "/ORDER_STATUS/codes";

	@Test
	@DisplayName("공통코드: 사용자는 사용하는 코드만 정렬 순서대로 본다. includeDisabled면 사용 안 함도, 지운 코드는 빼고")
	void createAndRead() {
		post(GROUPS, "{\"groupCode\":\"ORDER_STATUS\",\"name\":\"주문 상태\"}");
		post(ADMIN_CODES, "{\"code\":\"PAID\",\"name\":\"결제 완료\",\"sortOrder\":20}");
		post(ADMIN_CODES, "{\"code\":\"READY\",\"name\":\"주문 접수\",\"sortOrder\":10}");
		assertThat(admin(mvc.put().uri(ADMIN_CODES + "/PAID")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"결제 완료\",\"sortOrder\":20,\"enabled\":false}"))).hasStatus(200);

		MvcTestResult codes = mvc.get().uri("/api/v1/codes/ORDER_STATUS")
			.header(HttpHeaders.AUTHORIZATION, bearer(3L, "USER")).exchange();

		assertThat(codes).hasStatus(200);
		assertThat(codes).bodyJson().extractingPath("$.length()").isEqualTo(1);
		assertThat(codes).bodyJson().extractingPath("$[0].code").isEqualTo("READY");
		assertThat(admin(mvc.get().uri(ADMIN_CODES))).bodyJson().extractingPath("$.length()").isEqualTo(2);

		MvcTestResult all = mvc.get().uri("/api/v1/codes/ORDER_STATUS?includeDisabled=true")
			.header(HttpHeaders.AUTHORIZATION, bearer(3L, "USER")).exchange();
		assertThat(all).hasStatus(200);
		assertThat(all).bodyJson().extractingPath("$.length()").isEqualTo(2);
		assertThat(all).bodyJson().extractingPath("$[1].code").isEqualTo("PAID");
		assertThat(all).bodyJson().extractingPath("$[1].enabled").isEqualTo(false);

		assertThat(admin(mvc.delete().uri(ADMIN_CODES + "/PAID"))).hasStatus(204);
		assertThat(mvc.get().uri("/api/v1/codes/ORDER_STATUS?includeDisabled=true")
			.header(HttpHeaders.AUTHORIZATION, bearer(3L, "USER"))).bodyJson().extractingPath("$.length()")
			.isEqualTo(1);
	}

	@Test
	@DisplayName("공통코드: 같은 코드는 R002, 없는 그룹은 R001, 지운 코드는 다시 만들 수 있다")
	void duplicateAndDelete() {
		post(GROUPS, "{\"groupCode\":\"ORDER_STATUS\",\"name\":\"주문 상태\"}");
		post(ADMIN_CODES, "{\"code\":\"PAID\",\"name\":\"결제 완료\",\"sortOrder\":10}");

		assertThat(post(ADMIN_CODES, "{\"code\":\"PAID\",\"name\":\"중복\",\"sortOrder\":10}"))
			.hasStatus(409).bodyJson().extractingPath("$.code").isEqualTo("R002");
		assertThat(admin(mvc.get().uri(GROUPS + "/NONE/codes")))
			.hasStatus(404).bodyJson().extractingPath("$.code").isEqualTo("R001");
		assertThat(admin(mvc.delete().uri(ADMIN_CODES + "/PAID"))).hasStatus(204);
		assertThat(post(ADMIN_CODES, "{\"code\":\"PAID\",\"name\":\"결제 완료\",\"sortOrder\":10}")).hasStatus(201);
	}

	@Test
	@DisplayName("공통코드: 관리 API는 관리자만 (A005)")
	void adminOnly() {
		assertThat(mvc.get().uri(GROUPS).header(HttpHeaders.AUTHORIZATION, bearer(3L, "USER")))
			.hasStatus(403).bodyJson().extractingPath("$.code").isEqualTo("A005");
	}

	private MvcTestResult post(String uri, String json) {
		return admin(mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private MvcTestResult admin(
		org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(2L, "ADMIN")).exchange();
	}
}
