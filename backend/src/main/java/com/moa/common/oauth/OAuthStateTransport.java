package com.moa.common.oauth;

import java.time.Duration;
import java.util.Arrays;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.moa.common.security.token.AuthProperties;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 소셜 인증의 state 값을 쿠키로 둔다. 콜백은 제공자에서 넘어오는 교차 사이트 이동이라 SameSite=Lax로 둔다
 * (Strict면 콜백 요청에 쿠키가 실리지 않는다).
 */
@Component
@RequiredArgsConstructor
public class OAuthStateTransport {

	public static final String COOKIE_NAME = "oauth_state";
	private static final String COOKIE_PATH = "/api/v1/auth/oauth";
	private static final String SAME_SITE = "Lax";
	private static final Duration MAX_AGE = Duration.ofMinutes(10);

	private final AuthProperties authProperties;

	/** @return 쿠키의 state. 없으면 null */
	public String read(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		return Arrays.stream(cookies)
			.filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
			.map(Cookie::getValue)
			.findFirst()
			.orElse(null);
	}

	public void write(HttpServletResponse response, String state) {
		response.addHeader(HttpHeaders.SET_COOKIE, cookie(state, MAX_AGE));
	}

	public void clear(HttpServletResponse response) {
		response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO));
	}

	private String cookie(String value, Duration maxAge) {
		return ResponseCookie.from(COOKIE_NAME, value)
			.httpOnly(true)
			.secure(authProperties.refreshCookie().secure())
			.sameSite(SAME_SITE)
			.path(COOKIE_PATH)
			.maxAge(maxAge)
			.build()
			.toString();
	}
}
