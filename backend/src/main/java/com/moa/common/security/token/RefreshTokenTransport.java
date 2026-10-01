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
 * 리프레시 토큰을 어디에 담아 주고받는지 정한다. 웹과 앱의 차이는 이 클래스에만 있다.
 * <ul>
 *   <li>웹: HttpOnly 쿠키. 스크립트가 읽지 못한다</li>
 *   <li>앱(X-Client-Type: app): 응답·요청 본문. 앱은 보안 저장소(Keychain/Keystore)에 둔다</li>
 * </ul>
 * 발급·회전·폐기 로직은 AuthTokenService 하나다.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenTransport {

	public static final String CLIENT_TYPE_HEADER = "X-Client-Type";
	private static final String APP = "app";

	private final AuthProperties authProperties;

	public boolean isAppClient(HttpServletRequest request) {
		return authProperties.appClientEnabled() && APP.equalsIgnoreCase(request.getHeader(CLIENT_TYPE_HEADER));
	}

	/**
	 * @return 앱이면 본문에 담을 리프레시 토큰, 웹이면 null (쿠키로 보냈다)
	 */
	public String write(HttpServletRequest request, HttpServletResponse response, IssuedTokens tokens) {
		if (isAppClient(request)) {
			return tokens.refreshToken();
		}
		response.addHeader(HttpHeaders.SET_COOKIE, cookie(tokens.refreshToken(), tokens.refreshTokenTtl()));
		return null;
	}

	/**
	 * @param bodyValue 요청 본문의 refreshToken. 앱만 쓴다
	 */
	public String read(HttpServletRequest request, String bodyValue) {
		if (isAppClient(request)) {
			return bodyValue;
		}
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		return Arrays.stream(cookies)
			.filter(cookie -> authProperties.refreshCookie().name().equals(cookie.getName()))
			.map(Cookie::getValue)
			.findFirst()
			.orElse(null);
	}

	public void clear(HttpServletRequest request, HttpServletResponse response) {
		if (!isAppClient(request)) {
			response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO));
		}
	}

	private String cookie(String value, Duration maxAge) {
		AuthProperties.RefreshCookie settings = authProperties.refreshCookie();
		return ResponseCookie.from(settings.name(), value)
			.httpOnly(true)
			.secure(settings.secure())
			.sameSite(settings.sameSite())
			.path(settings.path())
			.maxAge(maxAge)
			.build()
			.toString();
	}
}
