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
 * 가입 이메일 인증 링크. 링크 토큰은 해시만 둔다. 한 번 쓰면(usedAt) 다시 쓸 수 없다.
 */
@Getter
@Entity
@Table(name = "email_verifications")
@HardDelete(reason = "한 번 쓰면 끝나는 인증 링크다. 만료 뒤 정리한다")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "sign_up_id")
	private Long signUpId;

	@Column(name = "email", nullable = false)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Column(name = "token_hash", nullable = false, length = 64)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "used_at")
	private LocalDateTime usedAt;
}
