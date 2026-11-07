package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;
import com.moa.api.application.v1.auth.AuthTokenApplication;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.Role;
import com.moa.common.security.token.IssuedTokens;
import com.moa.common.security.token.RefreshTokenTransport;
import com.moa.support.AcceptanceTest;
import com.moa.support.TestUsers;

import jakarta.servlet.http.Cookie;

/**
 * 토큰 재발급·로그아웃.
 */
class AuthTokenAcceptanceTest extends AcceptanceTest {

	private static final String COOKIE = "refresh_token";
	private static final String REFRESH = "/api/v1/auth/token/refresh";
	private static final String LOGOUT = "/api/v1/auth/logout";

	@Autowired
	private AuthTokenApplication authTokenApplication;

	@Autowired
	private TestUsers testUsers;

	@Test
	@DisplayName("토큰 재발급: 웹은 쿠키의 리프레시 토큰으로 재발급받고, 새 리프레시 토큰은 쿠키로만 받는다")
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
	@DisplayName("토큰 재발급: 앱은 본문으로 리프레시 토큰을 주고받는다")
	void refreshApp() {
		IssuedTokens login = login();

		MvcTestResult result = refreshAsApp(login.refreshToken());

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$.refreshToken").isNotEqualTo(login.refreshToken());
		assertThat(result.getResponse().getCookie(COOKIE)).isNull();
	}

	@Test
	@DisplayName("토큰 재발급: 이미 쓴 리프레시 토큰이 다시 오면 A004, 그 사용자의 토큰은 모두 폐기된다")
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
	@DisplayName("토큰 재발급: 없는 리프레시 토큰은 A003")
	void unknownToken() {
		assertThat(refreshAsApp("unknown"))
			.hasStatus(401)
			.bodyJson().extractingPath("$.code").isEqualTo("A003");
		assertThat(mvc.post().uri(REFRESH))
			.hasStatus(401)
			.bodyJson().extractingPath("$.code").isEqualTo("A003");
	}

	@Test
	@DisplayName("로그아웃: 로그아웃하면 쿠키를 지우고, 그 토큰으로는 재발급할 수 없다")
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
	@DisplayName("로그아웃: 토큰이 없어도 로그아웃은 성공한다")
	void logoutWithoutToken() {
		assertThat(mvc.post().uri(LOGOUT)).hasStatus(204);
	}

	/**
	 * 로그인 API는 사용자 도메인이 만든다. 여기서는 인증에 성공했다고 보고 바로 발급한다.
	 * 사용자 도메인이 생기면 발급 전에 계정 행을 만든다. 재발급이 계정을 다시 읽고, FK가 있으면 저장도 실패한다.
	 */
	private IssuedTokens login() {
		testUsers.create(7L, "user7", "password7!", Role.USER);
		return authTokenApplication.issue(new AuthPrincipal(7L, Set.of("USER")));
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
