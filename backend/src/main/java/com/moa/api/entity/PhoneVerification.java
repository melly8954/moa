package com.moa.api.entity;

import java.time.LocalDateTime;

import com.moa.common.entity.BaseTimeEntity;
import com.moa.common.entity.HardDelete;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 가입 휴대폰 인증 번호. 번호는 해시만 둔다. 하루 발송 횟수를 세는 데도 쓴다.
 */
@Getter
@Entity
@Table(name = "phone_verifications")
@HardDelete(reason = "한 번 쓰면 끝나는 인증 번호다. 하루 발송 횟수를 센 뒤 정리한다")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhoneVerification extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "sign_up_id")
	private Long signUpId;

	@Column(name = "phone_encrypted", nullable = false)
	private String phoneEncrypted;

	@Column(name = "phone_hmac", nullable = false, length = 64)
	private String phoneHmac;

	@Column(name = "code_hash", nullable = false, length = 64)
	private String codeHash;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "failed_attempts", nullable = false)
	private int failedAttempts;

	@Column(name = "verified_at")
	private LocalDateTime verifiedAt;
}
