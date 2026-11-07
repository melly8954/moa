package com.moa.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import com.moa.support.AcceptanceTest;

/**
 * 권한: @RequirePermission, ADMIN은 모든 권한, 역할 계층(ADMIN > USER), @CurrentUser, 내 권한
 */
class PermissionAcceptanceTest extends AcceptanceTest {

	private static final String PERMISSION = "/test/items/permission";
	private static final String USER_ONLY = "/test/items/user-only";
	private static final String MY_PERMISSIONS = "/api/v1/users/me/permissions";

	@Test
	@DisplayName("권한: ADMIN은 권한이 필요한 API를 부르고, @CurrentUser로 자기 ID를 받는다")
	void adminHasEveryPermission() {
		assertThat(mvc.get().uri(PERMISSION).header(HttpHeaders.AUTHORIZATION, bearer(2L, "ADMIN")))
			.hasStatus(200)
			.bodyText().isEqualTo("2");
	}

	@Test
	@DisplayName("권한: 권한이 없는 사용자는 A005, 로그인하지 않았으면 A001")
	void userWithoutPermission() {
		assertThat(mvc.get().uri(PERMISSION).header(HttpHeaders.AUTHORIZATION, bearer(3L, "USER")))
			.hasStatus(403)
			.bodyJson().extractingPath("$.code").isEqualTo("A005");
		assertThat(mvc.get().uri(PERMISSION))
			.hasStatus(401)
			.bodyJson().extractingPath("$.code").isEqualTo("A001");
	}

	@Test
	@DisplayName("권한: 내 권한은 ADMIN이면 등록된 전부, 권한을 받지 않은 사용자는 빈 목록")
	void myPermissions() {
		assertThat(mvc.get().uri(MY_PERMISSIONS).header(HttpHeaders.AUTHORIZATION, bearer(2L, "ADMIN")))
			.hasStatus(200)
			.bodyJson().extractingPath("$.permissions").asArray().contains("SAMPLE_MANAGE");
		assertThat(mvc.get().uri(MY_PERMISSIONS).header(HttpHeaders.AUTHORIZATION, bearer(3L, "USER")))
			.hasStatus(200)
			.bodyJson().extractingPath("$.permissions").asArray().isEmpty();
	}

	@Test
	@DisplayName("역할: 관리자는 USER 역할도 가진다 (역할 계층)")
	void adminInheritsUser() {
		assertThat(mvc.get().uri(USER_ONLY).header(HttpHeaders.AUTHORIZATION, bearer(2L, "ADMIN")))
			.hasStatus(200);
		assertThat(mvc.get().uri(USER_ONLY).header(HttpHeaders.AUTHORIZATION, bearer(3L, "USER")))
			.hasStatus(200);
	}
}
