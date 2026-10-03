package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;
import com.moa.api.application.v1.AuthTokenApplication;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.token.IssuedTokens;
import com.moa.common.security.token.RefreshTokenTransport;
import com.moa.support.AcceptanceTest;

import jakarta.servlet.http.Cookie;

/**
 * 토큰 재발급·로그아웃. AC 번호는 키트 문서 조각 auth-token의 FR 초안과 같다.
 * 프로젝트에 조각을 넣고 FR ID가 정해지면 표시 이름의 [auth-token-refresh], [auth-token-logout]을 그 ID로 바꾼다.
 * login()은 회원 행을 먼저 만든다 (FR-MEM-001, APPLY.md 적용 후 할 일 3).
 */
class AuthTokenAcceptanceTest extends AcceptanceTest {

	private static final String COOKIE = "refresh_token";
	private static final String REFRESH = "/api/v1/auth/token/refresh";
	private static final String LOGOUT = "/api/v1/auth/logout";
	private static final long MEMBER_ID = 7L;

	@Autowired
	private AuthTokenApplication authTokenApplication;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("[auth-token-refresh] AC-1: 웹은 쿠키의 리프레시 토큰으로 재발급받고, 새 리프레시 토큰은 쿠키로만 받는다")
	void refreshWeb() {
		IssuedTokens login = login();

		MvcTestResult result = mvc.post().uri(REFRESH).cookie(new Cookie(COOKIE, login.refreshToken())).exchange();

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().doesNotHavePath("$.refreshToken");
		Cookie rotated = result.getResponse().getCookie(COOKIE);
		assertThat(rotated).isNotNull();
		assertThat(rotated.isHttpOnly()).isTrue();
		assertThat(rotated.getValue()).isNotEqualTo(login.refreshToken());
		String accessToken = JsonPath.read(body(result), "$.accessToken");
		assertThat(mvc.get().uri("/test/items/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.hasStatus(200)
			.bodyText().isEqualTo("7");
	}

	@Test
	@DisplayName("[auth-token-refresh] AC-2: 앱은 본문으로 리프레시 토큰을 주고받는다")
	void refreshApp() {
		IssuedTokens login = login();

		MvcTestResult result = refreshAsApp(login.refreshToken());

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$.refreshToken").isNotEqualTo(login.refreshToken());
		assertThat(result.getResponse().getCookie(COOKIE)).isNull();
	}

	@Test
	@DisplayName("[auth-token-refresh] AC-3: 이미 쓴 리프레시 토큰이 다시 오면 A004, 그 사용자의 토큰은 모두 폐기된다")
	void reuseDetection() {
		IssuedTokens login = login();
		String rotated = JsonPath.read(body(refreshAsApp(login.refreshToken())), "$.refreshToken");

		assertThat(refreshAsApp(login.refreshToken()))
			.hasStatus(401)
			.bodyJson().extractingPath("$.code").isEqualTo("A004");
		assertThat(refreshAsApp(rotated))
			.hasStatus(401)
			.bodyJson().extractingPath("$.code").isEqualTo("A003");
	}

	@Test
	@DisplayName("[auth-token-refresh] AC-4: 없는 리프레시 토큰은 A003")
	void unknownToken() {
		assertThat(refreshAsApp("unknown"))
			.hasStatus(401)
			.bodyJson().extractingPath("$.code").isEqualTo("A003");
		assertThat(mvc.post().uri(REFRESH))
			.hasStatus(401)
			.bodyJson().extractingPath("$.code").isEqualTo("A003");
	}

	@Test
	@DisplayName("[auth-token-logout] AC-1: 로그아웃하면 쿠키를 지우고, 그 토큰으로는 재발급할 수 없다")
	void logout() {
		IssuedTokens login = login();

		MvcTestResult result = mvc.post().uri(LOGOUT).cookie(new Cookie(COOKIE, login.refreshToken())).exchange();

		assertThat(result).hasStatus(204);
		assertThat(result.getResponse().getCookie(COOKIE).getMaxAge()).isZero();
		assertThat(mvc.post().uri(REFRESH).cookie(new Cookie(COOKIE, login.refreshToken())))
			.hasStatus(401)
			.bodyJson().extractingPath("$.code").isEqualTo("A003");
	}

	@Test
	@DisplayName("[auth-token-logout] AC-2: 토큰이 없어도 로그아웃은 성공한다")
	void logoutWithoutToken() {
		assertThat(mvc.post().uri(LOGOUT)).hasStatus(204);
	}

	/**
	 * 로그인 API를 거치지 않고 인증에 성공했다고 보고 바로 발급한다.
	 * 재발급이 회원을 다시 읽으므로 발급 전에 회원 행(ACTIVE)을 먼저 만든다.
	 */
	private IssuedTokens login() {
		jdbcTemplate.update("insert into members (id, email, nickname, role, status, created_at, created_by, "
			+ "updated_at, updated_by) values (?, ?, ?, 'MEMBER', 'ACTIVE', now(6), 1, now(6), 1)",
			MEMBER_ID, "token-user@moa.test", "token_user");
		return authTokenApplication.issue(new AuthPrincipal(MEMBER_ID, Set.of("USER")));
	}

	private MvcTestResult refreshAsApp(String refreshToken) {
		return mvc.post().uri(REFRESH)
			.header(RefreshTokenTransport.CLIENT_TYPE_HEADER, "app")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"refreshToken\":\"" + refreshToken + "\"}")
			.exchange();
	}

	private String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString();
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
