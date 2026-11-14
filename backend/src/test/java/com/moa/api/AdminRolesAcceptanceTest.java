package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;

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
 * 운영자 역할. 스펙 specs/003-admin-roles의 수용 시나리오다
 */
class AdminRolesAcceptanceTest extends AcceptanceTest {

	private static final long OPERATOR_ID = 10L;

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
		testUsers.create(OPERATOR_ID, "operator", "operator123!", Role.USER);

		MvcTestResult role = asAdmin(mvc.post().uri("/api/v1/admin/roles").contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"SECURITY\",\"name\":\"보안 담당\",\"permissions\":[\"LOGIN_HISTORY_READ\"]}"));
		assertThat(role).hasStatus(201);
		Number roleId = JsonPath.read(body(role), "$.id");

		assertThat(asAdmin(mvc.put().uri("/api/v1/admin/users/" + OPERATOR_ID + "/roles")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"roleIds\":[" + roleId + "]}")))
			.hasStatus2xxSuccessful();
	}

	@Test
	@DisplayName("US1-AC1 접속 이력 조회 역할만 받은 운영자는 관리 메뉴에 접속 이력만 본다")
	void operatorSeesOnlyLoginHistoryMenu() {
		MvcTestResult result = asOperator(mvc.get().uri("/api/v1/menus/me"));

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$[*].name").asArray()
			.containsExactly("접속 이력");
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

	private MvcTestResult asAdmin(MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(2L, "ADMIN")).exchange();
	}

	private MvcTestResult asOperator(MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(OPERATOR_ID, "USER")).exchange();
	}

	private String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString();
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
