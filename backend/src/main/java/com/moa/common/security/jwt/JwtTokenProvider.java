package com.moa.common.security.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.security.AuthPrincipal;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * 액세스 토큰(JWT, HS256)을 만들고 검증한다. 리프레시 토큰은 JWT가 아니다 (RefreshTokenService).
 */
@Component
public class JwtTokenProvider {

	private static final String ROLES_CLAIM = "roles";

	private final JwtProperties properties;
	private final SecretKey key;

	public JwtTokenProvider(JwtProperties properties) {
		this.properties = properties;
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public String createAccessToken(AuthPrincipal principal) {
		Instant now = Instant.now();
		return Jwts.builder()
			.issuer(properties.issuer())
			.subject(String.valueOf(principal.userId()))
			.claim(ROLES_CLAIM, List.copyOf(principal.roles()))
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plus(properties.accessTokenTtl())))
			.signWith(key)
			.compact();
	}

	public long accessTokenTtlSeconds() {
		return properties.accessTokenTtl().toSeconds();
	}

	/**
	 * @throws ServiceException 만료면 A002, 그 밖의 무효는 A001
	 */
	public AuthPrincipal parse(String token) {
		try {
			Claims claims = Jwts.parser()
				.verifyWith(key)
				.requireIssuer(properties.issuer())
				.build()
				.parseSignedClaims(token)
				.getPayload();
			return new AuthPrincipal(Long.valueOf(claims.getSubject()), readRoles(claims));
		} catch (ExpiredJwtException ex) {
			throw new ServiceException(ErrorCode.ACCESS_TOKEN_EXPIRED);
		} catch (JwtException | IllegalArgumentException ex) {
			throw new ServiceException(ErrorCode.UNAUTHORIZED);
		}
	}

	private Set<String> readRoles(Claims claims) {
		Object value = claims.get(ROLES_CLAIM);
		Set<String> roles = new HashSet<>();
		if (value instanceof Collection<?> collection) {
			collection.forEach(role -> roles.add(String.valueOf(role)));
		}
		return roles;
	}
}
