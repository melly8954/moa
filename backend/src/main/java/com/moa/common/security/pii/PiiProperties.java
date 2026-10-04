package com.moa.common.security.pii;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * 개인정보 보호 키. 값은 환경변수로만 받는다 (운영 프로필은 기본값 없이 시작할 때 검사한다).
 *
 * @param encryptionKey 휴대폰 번호 원문 암호화(AES-GCM) 키 재료 (PII_ENCRYPTION_KEY)
 * @param hmacKey 휴대폰 번호 중복 검사(HMAC-SHA256) 키 (PII_HMAC_KEY)
 */
@Validated
@ConfigurationProperties("psw.pii")
public record PiiProperties(@NotBlank String encryptionKey, @NotBlank String hmacKey) {
}
