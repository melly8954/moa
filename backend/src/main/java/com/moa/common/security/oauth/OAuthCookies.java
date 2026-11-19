package com.moa.common.security.oauth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.moa.common.security.jwt.JwtProperties;
import com.moa.common.security.token.AuthProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 소셜 로그인의 짧은 상태를 서명한 HttpOnly 쿠키로 둔다 (specs/010-social-login research R3·R5, data-model "저장하지 않는 상태").
 * 서버에 세션을 두지 않는다. 서명 키는 JWT 키를 쓰고 발급자(iss)와 용도(typ)로 액세스 토큰과 나눈다.
 * <ul>
 *   <li>oauth_state: 제공자에서 돌아오는 최상위 GET에 붙어야 하므로 SameSite=Lax</li>
 *   <li>oauth_signup: 같은 사이트의 가입 요청에만 쓰므로 SameSite=Strict</li>
 * </ul>
 */
@Component
public class OAuthCookies {

	public static final String STATE_COOKIE = "oauth_state";
	public static final String SIGNUP_COOKIE = "oauth_signup";
	static final String PATH = "/api/v1/auth/oauth";
	static final Duration TTL = Duration.ofMinutes(10);
	private static final String TYP = "typ";

	private final SecretKey key;
	private final String issuer;
	private final boolean secure;

	public OAuthCookies(JwtProperties jwtProperties, AuthProperties authProperties) {
		this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
		this.issuer = jwtProperties.issuer() + ":oauth";
		this.secure = authProperties.refreshCookie().secure();
	}

	public void writeState(HttpServletResponse response, OAuthState state) {
		String value = sign(STATE_COOKIE, Jwts.claims()
			.add("provider", state.provider().name())
			.add("state", state.state())
			.add("nonce", state.nonce())
			.add("verifier", state.codeVerifier())
			.add("mode", state.mode().name())
			.add("next", state.next())
			.build());
		write(response, STATE_COOKIE, value, "Lax", TTL);
	}

	/** 서명·만료·용도가 맞지 않으면 빈 값 */
	public Optional<OAuthState> readState(HttpServletRequest request) {
		return read(request, STATE_COOKIE, claims -> new OAuthState(
			SocialProvider.valueOf(claims.get("provider", String.class)),
			claims.get("state", String.class),
			claims.get("nonce", String.class),
			claims.get("verifier", String.class),
			OAuthMode.valueOf(claims.get("mode", String.class)),
			claims.get("next", String.class)));
	}

	public void clearState(HttpServletResponse response) {
		write(response, STATE_COOKIE, "", "Lax", Duration.ZERO);
	}

	public void writeSignup(HttpServletResponse response, OAuthSignup signup) {
		String value = sign(SIGNUP_COOKIE, Jwts.claims()
			.add("provider", signup.provider().name())
			.add("sub", signup.subject())
			.add("email", signup.email())
			.add("name", signup.name())
			.add("next", signup.next())
			.build());
		write(response, SIGNUP_COOKIE, value, "Strict", TTL);
	}

	/** 서명·만료·용도가 맞지 않으면 빈 값 */
	public Optional<OAuthSignup> readSignup(HttpServletRequest request) {
		return read(request, SIGNUP_COOKIE, claims -> new OAuthSignup(
			SocialProvider.valueOf(claims.get("provider", String.class)),
			claims.getSubject(),
			claims.get("email", String.class),
			claims.get("name", String.class),
			claims.get("next", String.class)));
	}

	public void clearSignup(HttpServletResponse response) {
		write(response, SIGNUP_COOKIE, "", "Strict", Duration.ZERO);
	}

	private String sign(String typ, Claims claims) {
		Instant now = Instant.now();
		return Jwts.builder()
			.claims(claims)
			.issuer(issuer)
			.claim(TYP, typ)
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plus(TTL)))
			.signWith(key)
			.compact();
	}

	private <T> Optional<T> read(HttpServletRequest request, String name, Function<Claims, T> mapper) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return Optional.empty();
		}
		return Arrays.stream(cookies)
			.filter(cookie -> name.equals(cookie.getName()))
			.map(Cookie::getValue)
			.filter(value -> value != null && !value.isBlank())
			.findFirst()
			.flatMap(value -> parse(value, name, mapper));
	}

	private <T> Optional<T> parse(String value, String typ, Function<Claims, T> mapper) {
		try {
			Claims claims = Jwts.parser()
				.verifyWith(key)
				.requireIssuer(issuer)
				.require(TYP, typ)
				.build()
				.parseSignedClaims(value)
				.getPayload();
			return Optional.of(mapper.apply(claims));
		} catch (JwtException | IllegalArgumentException | NullPointerException ex) {
			return Optional.empty();
		}
	}

	private void write(HttpServletResponse response, String name, String value, String sameSite, Duration maxAge) {
		response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(name, value)
			.httpOnly(true)
			.secure(secure)
			.sameSite(sameSite)
			.path(PATH)
			.maxAge(maxAge)
			.build()
			.toString());
	}
}
