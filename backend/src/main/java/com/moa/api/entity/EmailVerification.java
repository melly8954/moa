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

	/**
	 * @param signUpId 카카오가 이메일을 주지 않아 이어 온 가입이면 그 가입 진행 ID, 아니면 null
	 */
	public EmailVerification(Long signUpId, String email, String passwordHash, String tokenHash,
		LocalDateTime expiresAt) {
		this.signUpId = signUpId;
		this.email = email;
		this.passwordHash = passwordHash;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	/** 아직 쓰지 않았고 만료되지 않았는지 */
	public boolean isUsable(LocalDateTime now) {
		return usedAt == null && expiresAt.isAfter(now);
	}

	public void markUsed(LocalDateTime now) {
		this.usedAt = now;
	}

	/** 발송에 실패한 링크를 쓸 수 없게 한다 */
	public void expire(LocalDateTime now) {
		this.expiresAt = now;
	}
}
