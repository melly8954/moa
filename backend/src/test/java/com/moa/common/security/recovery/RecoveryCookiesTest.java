package com.moa.common.security.recovery;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.moa.common.security.jwt.JwtProperties;
import com.moa.common.security.oauth.OAuthCookies;
import com.moa.common.security.oauth.OAuthMode;
import com.moa.common.security.oauth.OAuthState;
import com.moa.common.security.oauth.SocialProvider;
import com.moa.common.security.token.AuthProperties;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;

/** 복구 쿠키 (specs/018-member-withdrawal research R5): 위조·만료·용도 다름은 읽히지 않는다 */
class RecoveryCookiesTest {

	private static final String SECRET = "test-secret-test-secret-test-secret-0123";
	private static final AuthProperties AUTH = new AuthProperties(
		new AuthProperties.RefreshCookie("refresh_token", "/api/v1/auth", false, "Lax"), false);
	private static final RecoveryTicket TICKET = new RecoveryTicket(10L,
		LocalDateTime.of(2026, 11, 26, 12, 0, 0, 123456000));

	private final RecoveryCookies cookies = new RecoveryCookies(jwt(SECRET), AUTH);

	@Test
	@DisplayName("쓴 쿠키를 다시 읽으면 회원 ID와 탈퇴 시각(마이크로초까지)이 같다. HttpOnly·Strict·복구 경로·10분이다")
	void roundTrip() {
		MockHttpServletResponse response = new MockHttpServletResponse();
		cookies.write(response, TICKET);

		String header = response.getHeader("Set-Cookie");
		assertThat(header).contains("HttpOnly", "SameSite=Strict", "Path=/api/v1/auth/recovery", "Max-Age=600");
		assertThat(cookies.read(requestWith(response.getCookie(RecoveryCookies.COOKIE)))).contains(TICKET);
	}

	@Test
	@DisplayName("다른 키로 서명했거나 값을 바꾼 쿠키는 읽히지 않는다 (위조)")
	void forged() {
		MockHttpServletResponse response = new MockHttpServletResponse();
		new RecoveryCookies(jwt("other-secret-other-secret-other-secret-99"), AUTH).write(response, TICKET);
		Cookie other = response.getCookie(RecoveryCookies.COOKIE);

		assertThat(cookies.read(requestWith(other))).isEmpty();
		assertThat(cookies.read(requestWith(new Cookie(RecoveryCookies.COOKIE, "not-a-token")))).isEmpty();
		assertThat(cookies.read(new MockHttpServletRequest())).isEmpty();
	}

	@Test
	@DisplayName("만료된 쿠키(10분 지남)는 읽히지 않는다")
	void expired() {
		Instant past = Instant.now().minus(Duration.ofMinutes(11));
		String value = Jwts.builder()
			.subject("10")
			.claim("withdrawnAt", TICKET.withdrawnAt().toString())
			.issuer("moa-test:recovery")
			.claim("typ", RecoveryCookies.COOKIE)
			.issuedAt(Date.from(past))
			.expiration(Date.from(past.plus(Duration.ofMinutes(10))))
			.signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
			.compact();

		assertThat(cookies.read(requestWith(new Cookie(RecoveryCookies.COOKIE, value)))).isEmpty();
	}

	@Test
	@DisplayName("소셜 로그인 쿠키를 복구 쿠키 자리에 넣어도 읽히지 않는다 (용도 다름)")
	void otherPurpose() {
		MockHttpServletResponse response = new MockHttpServletResponse();
		new OAuthCookies(jwt(SECRET), AUTH).writeState(response,
			new OAuthState(SocialProvider.GOOGLE, "state", "nonce", "verifier", OAuthMode.LOGIN, null));
		String value = response.getCookie(OAuthCookies.STATE_COOKIE).getValue();

		assertThat(cookies.read(requestWith(new Cookie(RecoveryCookies.COOKIE, value)))).isEmpty();
	}

	@Test
	@DisplayName("지운 쿠키(Max-Age=0)를 쓴다")
	void clear() {
		MockHttpServletResponse response = new MockHttpServletResponse();
		cookies.clear(response);
		assertThat(response.getHeader("Set-Cookie")).contains(RecoveryCookies.COOKIE + "=", "Max-Age=0");
	}

	private static JwtProperties jwt(String secret) {
		return new JwtProperties(secret, "moa-test", Duration.ofMinutes(15), Duration.ofDays(14));
	}

	private static MockHttpServletRequest requestWith(Cookie cookie) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(cookie);
		return request;
	}
}
