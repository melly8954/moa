package com.moa.common.security.token;

import java.time.Duration;
import java.util.Arrays;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 가입 진행 토큰을 HttpOnly 쿠키로 주고받는다. 가입 단계 API(/api/v1/sign-ups, /api/v1/members)와
 * 소셜 콜백(/api/v1/auth/oauth)이 모두 받도록 경로는 /api/v1이다.
 */
@Component
@RequiredArgsConstructor
public class SignUpTokenTransport {

	public static final String COOKIE_NAME = "sign_up_token";
	private static final String COOKIE_PATH = "/api/v1";
	private static final String SAME_SITE = "Strict";

	private final AuthProperties authProperties;

	/** @return 쿠키의 토큰 원문. 없으면 null */
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

	/** 브라우저 세션 쿠키로 쓴다. 만료는 서버가 가입 진행의 만료 시각으로 판단한다 */
	public void write(HttpServletResponse response, String rawToken) {
		response.addHeader(HttpHeaders.SET_COOKIE, cookie(rawToken, null));
	}

	public void clear(HttpServletResponse response) {
		response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO));
	}

	private String cookie(String value, Duration maxAge) {
		ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(COOKIE_NAME, value)
			.httpOnly(true)
			.secure(authProperties.refreshCookie().secure())
			.sameSite(SAME_SITE)
			.path(COOKIE_PATH);
		if (maxAge != null) {
			builder.maxAge(maxAge);
		}
		return builder.build().toString();
	}
}
