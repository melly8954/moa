package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;
import com.moa.common.security.Role;
import com.moa.support.AcceptanceTest;
import com.moa.support.TestUsers;

import jakarta.servlet.http.Cookie;

/**
 * 아이디·비밀번호 로그인과 내 정보.
 */
class AuthLoginAcceptanceTest extends AcceptanceTest {

	private static final String LOGIN = "/api/v1/auth/login";
	private static final String ME = "/api/v1/users/me";

	@Autowired
	private TestUsers testUsers;

	@Test
	@DisplayName("로그인: 웹은 액세스 토큰을 본문으로, 리프레시 토큰을 HttpOnly 쿠키로 받고 내 정보를 볼 수 있다")
	void loginWeb() {
		testUsers.create(10L, "kim", "secret123!", Role.USER);

		MvcTestResult result = login("kim", "secret123!");

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().doesNotHavePath("$.refreshToken");
		Cookie cookie = result.getResponse().getCookie("refresh_token");
		assertThat(cookie).isNotNull();
		assertThat(cookie.isHttpOnly()).isTrue();
		String accessToken = JsonPath.read(body(result), "$.accessToken");
		MvcTestResult me = mvc.get().uri(ME).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).exchange();
		assertThat(me).hasStatus(200);
		assertThat(me).bodyJson().extractingPath("$.loginId").isEqualTo("kim");
		assertThat(me).bodyJson().extractingPath("$.role").isEqualTo("USER");
	}

	@Test
	@DisplayName("로그인: 앱은 리프레시 토큰을 본문으로 받는다")
	void loginApp() {
		testUsers.create(10L, "kim", "secret123!", Role.USER);

		MvcTestResult result = mvc.post().uri(LOGIN)
			.header("X-Client-Type", "app")
			.contentType(MediaType.APPLICATION_JSON)
			.content(json("kim", "secret123!"))
			.exchange();

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().hasPath("$.refreshToken");
		assertThat(result.getResponse().getCookie("refresh_token")).isNull();
	}

	@Test
	@DisplayName("로그인: 비밀번호가 틀리거나 없는 아이디면 같은 A006으로 거절한다")
	void wrongCredentials() {
		testUsers.create(10L, "kim", "secret123!", Role.USER);

		assertThat(login("kim", "wrong-password")).hasStatus(401).bodyJson().extractingPath("$.code").isEqualTo("A006");
		assertThat(login("nobody", "secret123!")).hasStatus(401).bodyJson().extractingPath("$.code").isEqualTo("A006");
	}

	@Test
	@DisplayName("로그인: 비밀번호가 없는 계정(시스템 계정 등)은 로그인할 수 없다")
	void accountWithoutPasswordCannotLogin() {
		testUsers.create(20L, "batch", null, Role.USER);

		assertThat(login("batch", "anything")).hasStatus(401).bodyJson().extractingPath("$.code").isEqualTo("A006");
	}

	@Test
	@DisplayName("로그인: 아이디나 비밀번호가 비면 V001")
	void blankInput() {
		assertThat(login("", "")).hasStatus(400).bodyJson().extractingPath("$.code").isEqualTo("V001");
	}

	@Test
	@DisplayName("내 정보: 로그인하지 않으면 A001")
	void meWithoutLogin() {
		assertThat(mvc.get().uri(ME)).hasStatus(401).bodyJson().extractingPath("$.code").isEqualTo("A001");
	}

	private MvcTestResult login(String loginId, String password) {
		return mvc.post().uri(LOGIN).contentType(MediaType.APPLICATION_JSON).content(json(loginId, password))
			.exchange();
	}

	private String json(String loginId, String password) {
		return "{\"loginId\":\"" + loginId + "\",\"password\":\"" + password + "\"}";
	}

	private String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString();
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
