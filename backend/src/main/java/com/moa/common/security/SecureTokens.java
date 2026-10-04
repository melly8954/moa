package com.moa.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 한 번 쓰는 값(가입 진행 토큰, 인증 링크 토큰, 인증 번호)을 만들고 해시한다. DB에는 해시만 둔다.
 */
public final class SecureTokens {

	private static final int TOKEN_BYTES = 32;
	private static final int CODE_BOUND = 1_000_000;
	private static final SecureRandom RANDOM = new SecureRandom();

	private SecureTokens() {
	}

	/** URL에 그대로 넣을 수 있는 무작위 토큰 */
	public static String newToken() {
		byte[] bytes = new byte[TOKEN_BYTES];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** 6자리 숫자 인증 번호 (앞자리 0 포함) */
	public static String newNumericCode() {
		return String.format("%06d", RANDOM.nextInt(CODE_BOUND));
	}

	/** SHA-256 16진수 (64자) */
	public static String sha256(String raw) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
