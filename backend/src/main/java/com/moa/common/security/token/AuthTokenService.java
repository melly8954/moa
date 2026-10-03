package com.moa.common.security.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.jwt.JwtProperties;
import com.moa.common.security.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 토큰 발급·재발급·폐기. 로그인 방식(비밀번호, 소셜 등)은 사용자 도메인이 정하고, 인증에 성공하면 issue를 부른다.
 *
 * <p>리프레시 토큰은 재발급할 때마다 새 토큰으로 바꾼다(회전). 이미 쓴 토큰이 다시 오면 탈취로 보고
 * 그 사용자의 토큰을 모두 폐기한다(재사용 감지).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthTokenService {

	private static final int REFRESH_TOKEN_BYTES = 32;
	private static final SecureRandom RANDOM = new SecureRandom();

	private final JwtTokenProvider jwtTokenProvider;
	private final JwtProperties jwtProperties;
	private final RefreshTokenRepository refreshTokenRepository;
	private final ObjectProvider<AuthPrincipalLoader> authPrincipalLoader;

	@Transactional
	public IssuedTokens issue(AuthPrincipal principal) {
		return issueTokens(principal, UUID.randomUUID().toString());
	}

	/**
	 * 로그인 세션을 새로 만들고 그 세션에 속한 토큰을 발급한다. 액세스 토큰에는 세션 ID를 sid 클레임으로 넣는다.
	 * 회원 도메인의 로그인(가입 완료, 이메일 인증 연결, 소셜 콜백, 비밀번호 로그인)은 이 메서드를 부른다.
	 */
	@Transactional
	public IssuedTokens issueWithNewSession(AuthPrincipal principal) {
		throw new UnsupportedOperationException("구현 전입니다");
	}

	/**
	 * @throws ServiceException 무효·만료·폐기면 A003, 이미 쓴 토큰이면 A004
	 */
	@Transactional(noRollbackFor = ServiceException.class)
	public IssuedTokens refresh(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			throw new ServiceException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
		// 같은 토큰으로 동시에 재발급하면 둘 다 성공하지 않도록 행을 잠근다
		RefreshToken token = refreshTokenRepository.findForUpdateByTokenHash(hash(rawRefreshToken))
			.orElseThrow(() -> new ServiceException(ErrorCode.INVALID_REFRESH_TOKEN));
		LocalDateTime now = LocalDateTime.now();
		if (token.isUsed()) {
			log.warn("리프레시 토큰 재사용 감지 userId={} familyId={}", token.getUserId(), token.getFamilyId());
			refreshTokenRepository.revokeAllByUserId(token.getUserId(), now);
			throw new ServiceException(ErrorCode.REFRESH_TOKEN_REUSED);
		}
		if (token.isRevoked() || token.isExpired(now)) {
			throw new ServiceException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
		// 사용자를 먼저 읽는다. 실패하면 토큰을 쓴 것으로 표시하지 않는다
		AuthPrincipal principal = loader().load(token.getUserId());
		token.markUsed(now);
		return issueTokens(principal, token.getFamilyId());
	}

	/** 로그아웃. 없는 토큰이어도 실패하지 않는다 */
	@Transactional
	public void revoke(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			return;
		}
		refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
			.ifPresent(token -> token.revoke(LocalDateTime.now()));
	}

	/** 비밀번호 변경, 탈퇴 등으로 모든 기기에서 로그아웃시킬 때 */
	@Transactional
	public void revokeAll(Long userId) {
		refreshTokenRepository.revokeAllByUserId(userId, LocalDateTime.now());
	}

	private IssuedTokens issueTokens(AuthPrincipal principal, String familyId) {
		String rawRefreshToken = newRawToken();
		refreshTokenRepository.save(new RefreshToken(principal.userId(), familyId, hash(rawRefreshToken),
			LocalDateTime.now().plus(jwtProperties.refreshTokenTtl())));
		return new IssuedTokens(jwtTokenProvider.createAccessToken(principal),
			jwtTokenProvider.accessTokenTtlSeconds(), rawRefreshToken, jwtProperties.refreshTokenTtl());
	}

	private AuthPrincipalLoader loader() {
		AuthPrincipalLoader loader = authPrincipalLoader.getIfAvailable();
		if (loader == null) {
			log.error("AuthPrincipalLoader 구현이 없습니다. 사용자 도메인에서 빈으로 등록해야 재발급할 수 있습니다.");
			throw new ServiceException(ErrorCode.INTERNAL_ERROR);
		}
		return loader;
	}

	private String newRawToken() {
		byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	static String hash(String raw) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
