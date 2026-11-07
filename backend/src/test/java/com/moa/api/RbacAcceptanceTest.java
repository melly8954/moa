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
import com.moa.support.TestUsers;

/**
 * 역할·권한 (선택 헬퍼 rbac)
 */
class RbacAcceptanceTest extends AcceptanceTest {

	private static final String ROLES = "/api/v1/admin/roles";
	private static final String USER_ROLES = "/api/v1/admin/users/3/roles";
	private static final String MY_PERMISSIONS = "/api/v1/users/me/permissions";

	@Autowired
	private TestUsers testUsers;

	/** 역할을 줄 사용자 (id 3) */
	@BeforeEach
	void createUser() {
		testUsers.create(3L, "user", "password1!", Role.USER);
	}

	@Test
	@DisplayName("rbac: 역할에 권한을 묶어 사용자에게 주면 그 권한의 API를 부르고, 빼면 바로 막힌다")
	void grantAndRevoke() {
		assertThat(asUser(mvc.get().uri(ROLES))).hasStatus(403);

		long manager = createRole("MANAGER", "[\"ROLE_MANAGE\"]");
		assertThat(replaceUserRoles("[" + manager + "]")).hasStatus(204);

		assertThat(asUser(mvc.get().uri(ROLES))).hasStatus(200);
		assertThat(asUser(mvc.get().uri(MY_PERMISSIONS))).bodyJson().extractingPath("$.permissions")
			.asArray().containsExactly("ROLE_MANAGE");
		assertThat(asAdmin(mvc.get().uri(USER_ROLES))).bodyJson().extractingPath("$[0].code").isEqualTo("MANAGER");
		assertThat(asAdmin(mvc.get().uri(ROLES))).bodyJson().extractingPath("$[0].userCount").isEqualTo(1);

		assertThat(replaceUserRoles("[]")).hasStatus(204);
		assertThat(asUser(mvc.get().uri(ROLES))).hasStatus(403).bodyJson().extractingPath("$.code").isEqualTo("A005");
	}

	@Test
	@DisplayName("rbac: 역할 권한을 바꾸면 바로 반영되고, 역할을 지우면 사용자에게서 빠진다")
	void modifyAndDelete() {
		long manager = createRole("MANAGER", "[]");
		replaceUserRoles("[" + manager + "]");
		assertThat(asUser(mvc.get().uri(ROLES))).hasStatus(403);

		assertThat(asAdmin(mvc.put().uri(ROLES + "/" + manager).contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"매니저\",\"permissions\":[\"ROLE_MANAGE\"]}")))
			.hasStatus(200).bodyJson().extractingPath("$.code").isEqualTo("MANAGER");
		assertThat(asUser(mvc.get().uri(ROLES))).hasStatus(200);

		assertThat(asAdmin(mvc.delete().uri(ROLES + "/" + manager))).hasStatus(204);
		assertThat(asUser(mvc.get().uri(ROLES))).hasStatus(403);
		assertThat(asAdmin(mvc.get().uri(USER_ROLES))).bodyJson().extractingPath("$.length()").isEqualTo(0);
		assertThat(asAdmin(mvc.get().uri(ROLES + "/" + manager))).hasStatus(404);
	}

	@Test
	@DisplayName("rbac: 코드 형식·등록되지 않은 권한 V001, 코드 중복 R002 (지운 역할의 코드는 다시 쓴다), 없는 역할·사용자 R001")
	void validation() {
		assertThat(postRole("manager", "[]")).hasStatus(400).bodyJson().extractingPath("$.code").isEqualTo("V001");
		assertThat(postRole("MANAGER", "[\"NO_SUCH\"]")).hasStatus(400)
			.bodyJson().extractingPath("$.code").isEqualTo("V001");

		long manager = createRole("MANAGER", "[]");
		assertThat(postRole("MANAGER", "[]")).hasStatus(409).bodyJson().extractingPath("$.code").isEqualTo("R002");
		asAdmin(mvc.delete().uri(ROLES + "/" + manager));
		assertThat(postRole("MANAGER", "[]")).hasStatus(201);

		assertThat(replaceUserRoles("[999999]")).hasStatus(404).bodyJson().extractingPath("$.code").isEqualTo("R001");
		assertThat(asAdmin(mvc.put().uri("/api/v1/admin/users/999999/roles").contentType(MediaType.APPLICATION_JSON)
			.content("{\"roleIds\":[]}"))).hasStatus(404);
	}

	@Test
	@DisplayName("rbac: ADMIN이 아니면 가진 권한까지만 다룬다. 없는 권한을 넣거나, 그런 역할을 주거나 지우면 A005")
	void noEscalation() {
		long manager = createRole("MANAGER", "[\"ROLE_MANAGE\"]");
		long coder = createRole("CODER", "[\"CODE_MANAGE\"]");
		replaceUserRoles("[" + manager + "]");

		assertThat(asUser(rolePost("SUPER", "[\"CODE_MANAGE\"]"))).hasStatus(403)
			.bodyJson().extractingPath("$.code").isEqualTo("A005");
		assertThat(asUser(rolePost("HELPER", "[\"ROLE_MANAGE\"]"))).hasStatus(201);
		assertThat(asUser(mvc.put().uri(USER_ROLES).contentType(MediaType.APPLICATION_JSON)
			.content("{\"roleIds\":[" + manager + "," + coder + "]}"))).hasStatus(403);
		assertThat(asUser(mvc.put().uri(USER_ROLES).contentType(MediaType.APPLICATION_JSON)
			.content("{\"roleIds\":[" + manager + "]}"))).hasStatus(204);
		assertThat(asUser(mvc.put().uri(ROLES + "/" + manager).contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"m\",\"permissions\":[\"ROLE_MANAGE\",\"CODE_MANAGE\"]}"))).hasStatus(403);
		assertThat(asUser(mvc.delete().uri(ROLES + "/" + coder))).hasStatus(403);
		assertThat(asUser(mvc.get().uri(MY_PERMISSIONS))).bodyJson().extractingPath("$.permissions")
			.asArray().containsExactly("ROLE_MANAGE");
	}

	@Test
	@DisplayName("rbac: 권한 목록은 코드에 등록된 것 전부이고, ADMIN은 역할 없이도 모든 권한을 갖는다")
	void permissionsAndAdmin() {
		assertThat(asAdmin(mvc.get().uri("/api/v1/admin/permissions"))).hasStatus(200)
			.bodyJson().extractingPath("$[*].key").asArray().contains("ROLE_MANAGE");
		assertThat(asAdmin(mvc.get().uri(MY_PERMISSIONS))).bodyJson().extractingPath("$.permissions")
			.asArray().contains("ROLE_MANAGE");
	}

	private long createRole(String code, String permissions) {
		MvcTestResult result = postRole(code, permissions);
		assertThat(result).hasStatus(201);
		return ((Number)JsonPath.read(body(result), "$.id")).longValue();
	}

	private MvcTestResult postRole(String code, String permissions) {
		return asAdmin(rolePost(code, permissions));
	}

	private MockMvcRequestBuilder rolePost(String code, String permissions) {
		return mvc.post().uri(ROLES).contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"" + code + "\",\"name\":\"" + code + "\",\"permissions\":" + permissions + "}");
	}

	private MvcTestResult replaceUserRoles(String roleIds) {
		return asAdmin(mvc.put().uri(USER_ROLES).contentType(MediaType.APPLICATION_JSON)
			.content("{\"roleIds\":" + roleIds + "}"));
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
