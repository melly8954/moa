package com.moa.common.security.pii;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

/**
 * 휴대폰 번호 보호 (docs/design/security.md 데이터 보호). 원문은 AES-GCM으로 암호화해 두고,
 * 중복 검사는 HMAC-SHA256 값으로 한다. 키 재료는 SHA-256으로 256비트 키로 만든다.
 */
@Component
public class PiiCipher {

	private static final String AES = "AES";
	private static final String TRANSFORMATION = "AES/GCM/NoPadding";
	private static final String HMAC = "HmacSHA256";
	private static final int IV_BYTES = 12;
	private static final int TAG_BITS = 128;
	private static final SecureRandom RANDOM = new SecureRandom();

	private final SecretKeySpec encryptionKey;
	private final SecretKeySpec hmacKey;

	public PiiCipher(PiiProperties properties) {
		this.encryptionKey = new SecretKeySpec(deriveKey(properties.encryptionKey()), AES);
		this.hmacKey = new SecretKeySpec(deriveKey(properties.hmacKey()), HMAC);
	}

	/** @return IV를 앞에 붙인 암호문의 Base64 */
	public String encrypt(String plain) {
		try {
			byte[] iv = new byte[IV_BYTES];
			RANDOM.nextBytes(iv);
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_BITS, iv));
			byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
				.put(iv).put(encrypted).array());
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("개인정보를 암호화하지 못했습니다.", ex);
		}
	}

	public String decrypt(String encoded) {
		try {
			ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(encoded));
			byte[] iv = new byte[IV_BYTES];
			buffer.get(iv);
			byte[] encrypted = new byte[buffer.remaining()];
			buffer.get(encrypted);
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_BITS, iv));
			return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("개인정보를 복호화하지 못했습니다.", ex);
		}
	}

	/** @return HMAC-SHA256 16진수 (64자) */
	public String hmac(String plain) {
		try {
			Mac mac = Mac.getInstance(HMAC);
			mac.init(hmacKey);
			return HexFormat.of().formatHex(mac.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("개인정보 해시를 만들지 못했습니다.", ex);
		}
	}

	private static byte[] deriveKey(String material) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8));
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
