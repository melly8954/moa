package com.moa.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.moa.common.logging.TraceIdFilter;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.jwt.JwtProperties;
import com.moa.common.security.jwt.JwtTokenProvider;
import com.moa.support.AcceptanceTest;

/**
 * 오류 응답 형식 (conventions.md REST 절)
 */
class ErrorResponseAcceptanceTest extends AcceptanceTest {

	@Autowired
	private JwtProperties jwtProperties;

	@Test
	@DisplayName("인증 없이 보호된 API를 부르면 401 A001, traceId는 응답 헤더와 같다")
	void unauthorized() {
		MvcTestResult result = mvc.get().uri("/test/items/1").exchange();

		assertThat(result).hasStatus(401);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("A001");
		assertThat(result).bodyJson().extractingPath("$.path").isEqualTo("/test/items/1");
		assertThat(result).bodyJson().extractingPath("$.traceId")
			.isEqualTo(result.getResponse().getHeader(TraceIdFilter.HEADER));
	}

	@Test
	@DisplayName("만료된 액세스 토큰은 401 A002")
	void expiredAccessToken() {
		JwtTokenProvider expiredProvider = new JwtTokenProvider(new JwtProperties(jwtProperties.secret(),
			jwtProperties.issuer(), Duration.ofSeconds(-1), jwtProperties.refreshTokenTtl()));
		String token = expiredProvider.createAccessToken(new AuthPrincipal(7L, null));

		assertThat(mvc.get().uri("/test/items/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.hasStatus(401)
			.bodyJson().extractingPath("$.code").isEqualTo("A002");
	}

	@Test
	@DisplayName("위조된 액세스 토큰은 401 A001")
	void invalidAccessToken() {
		assertThat(mvc.get().uri("/test/items/1").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
			.hasStatus(401)
			.bodyJson().extractingPath("$.code").isEqualTo("A001");
	}

	@Test
	@DisplayName("입력 검증 실패는 400 V001이고 필드별 사유를 담는다")
	void validationFailed() {
		MvcTestResult result = mvc.post().uri("/test/items")
			.header(HttpHeaders.AUTHORIZATION, bearer(7L))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"\"}")
			.exchange();

		assertThat(result).hasStatus(400);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("V001");
		assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("name");
	}

	@Test
	@DisplayName("V001이 아닌 오류에는 errors가 없다")
	void noErrorsFieldOtherwise() {
		assertThat(mvc.get().uri("/test/items/999").header(HttpHeaders.AUTHORIZATION, bearer(7L)))
			.hasStatus(404)
			.bodyJson().doesNotHavePath("$.errors");
	}

	@Test
	@DisplayName("없는 경로는 404 R001, 지원하지 않는 메서드는 405 V002")
	void routing() {
		assertThat(mvc.get().uri("/no-such-path").header(HttpHeaders.AUTHORIZATION, bearer(7L)))
			.hasStatus(404)
			.bodyJson().extractingPath("$.code").isEqualTo("R001");
		assertThat(mvc.put().uri("/test/items/1").header(HttpHeaders.AUTHORIZATION, bearer(7L)))
			.hasStatus(405)
			.bodyJson().extractingPath("$.code").isEqualTo("V002");
	}
}
