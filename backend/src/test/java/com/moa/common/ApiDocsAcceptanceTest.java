package com.moa.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.moa.support.AcceptanceTest;

/**
 * API 설명서 필수 칸 규칙: record 칸은 필수, @Nullable·NOT_REQUIRED만 선택 (RecordRequiredConverter)
 */
@TestPropertySource(properties = "springdoc.api-docs.enabled=true")
class ApiDocsAcceptanceTest extends AcceptanceTest {

	@Test
	@DisplayName("API 설명서: 응답 record의 늘 오는 칸은 required, @Nullable 칸은 빠진다")
	void responseRequired() {
		MvcTestResult docs = mvc.get().uri("/v3/api-docs").exchange();

		assertThat(docs).hasStatus(200);
		assertThat(docs).bodyJson().extractingPath("$.components.schemas.UserGetResponse.required").asArray()
			.containsExactlyInAnyOrder("id", "loginId", "name", "role", "passwordChangeRequired");
		assertThat(docs).bodyJson().extractingPath("$.components.schemas.ErrorResponse.required").asArray()
			.containsExactlyInAnyOrder("code", "message", "timestamp", "path");
		assertThat(docs).bodyJson().extractingPath("$.components.schemas.AuthRefreshResponse.required").asArray()
			.containsExactlyInAnyOrder("accessToken", "tokenType", "expiresIn");
	}

	@Test
	@DisplayName("API 설명서: 요청 record는 검증 어노테이션·REQUIRED 칸만 required, NOT_REQUIRED 칸은 빠진다")
	void requestRequired() {
		MvcTestResult docs = mvc.get().uri("/v3/api-docs").exchange();

		assertThat(docs).bodyJson().extractingPath("$.components.schemas.AuthLoginRequest.required").asArray()
			.containsExactlyInAnyOrder("loginId", "password");
		assertThat(docs).bodyJson().doesNotHavePath("$.components.schemas.AuthRefreshRequest.required");
	}
}
