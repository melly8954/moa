package com.moa.common.security.oauth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * state·nonce·PKCE verifier 만들기 (RFC 7636). 모두 32바이트 무작위의 base64url이다.
 */
public final class OAuthSecrets {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

	private OAuthSecrets() {
	}

	public static String random() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return ENCODER.encodeToString(bytes);
	}

	/** S256 code_challenge = base64url(SHA-256(verifier)) */
	public static String codeChallenge(String verifier) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
			return ENCODER.encodeToString(digest);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256을 쓸 수 없습니다", ex);
		}
	}
}
