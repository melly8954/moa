package com.moa.common.security.recovery;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

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
 * 탈퇴한 계정의 복구 쿠키 {@value #COOKIE} (specs/018-member-withdrawal research R5). 010 OAuthCookies와 같은 방식이다.
 * 회원 ID와 탈퇴 시각을 JWT 키로 서명하고, 발급자(…:recovery)와 용도(typ)로 액세스 토큰·소셜 쿠키와 나눈다.
 * 10분, HttpOnly, SameSite=Strict, 복구 API 경로에만 간다.
 */
@Component
public class RecoveryCookies {

	public static final String COOKIE = "withdrawal_recovery";
	static final String PATH = "/api/v1/auth/recovery";
	static final Duration TTL = Duration.ofMinutes(10);
	private static final String TYP = "typ";
	private static final String WITHDRAWN_AT = "withdrawnAt";

	private final SecretKey key;
	private final String issuer;
	private final boolean secure;

	public RecoveryCookies(JwtProperties jwtProperties, AuthProperties authProperties) {
		this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
		this.issuer = jwtProperties.issuer() + ":recovery";
		this.secure = authProperties.refreshCookie().secure();
	}

	public void write(HttpServletResponse response, RecoveryTicket ticket) {
		Instant now = Instant.now();
		String value = Jwts.builder()
			.subject(String.valueOf(ticket.userId()))
			.claim(WITHDRAWN_AT, ticket.withdrawnAt().toString())
			.issuer(issuer)
			.claim(TYP, COOKIE)
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plus(TTL)))
			.signWith(key)
			.compact();
		write(response, value, TTL);
	}

	/** 서명·만료·용도가 맞지 않으면 빈 값 */
	public Optional<RecoveryTicket> read(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return Optional.empty();
		}
		return Arrays.stream(cookies)
			.filter(cookie -> COOKIE.equals(cookie.getName()))
			.map(Cookie::getValue)
			.filter(value -> value != null && !value.isBlank())
			.findFirst()
			.flatMap(this::parse);
	}

	public void clear(HttpServletResponse response) {
		write(response, "", Duration.ZERO);
	}

	private Optional<RecoveryTicket> parse(String value) {
		try {
			Claims claims = Jwts.parser()
				.verifyWith(key)
				.requireIssuer(issuer)
				.require(TYP, COOKIE)
				.build()
				.parseSignedClaims(value)
				.getPayload();
			return Optional.of(new RecoveryTicket(Long.valueOf(claims.getSubject()),
				LocalDateTime.parse(claims.get(WITHDRAWN_AT, String.class))));
		} catch (JwtException | IllegalArgumentException | NullPointerException
			| DateTimeParseException ex) {
			return Optional.empty();
		}
	}

	private void write(HttpServletResponse response, String value, Duration maxAge) {
		response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(COOKIE, value)
			.httpOnly(true)
			.secure(secure)
			.sameSite("Strict")
			.path(PATH)
			.maxAge(maxAge)
			.build()
			.toString());
	}
}
