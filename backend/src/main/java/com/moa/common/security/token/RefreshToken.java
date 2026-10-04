package com.moa.common.security.token;

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
 * 리프레시 토큰. 원문은 저장하지 않고 SHA-256 해시만 저장한다.
 * 같은 로그인에서 회전으로 이어진 토큰은 familyId가 같다.
 */
@Getter
@Entity
@Table(name = "refresh_tokens")
@HardDelete(reason = "만료·폐기된 토큰은 보관할 이유가 없어 주기적으로 지운다")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "family_id", nullable = false, length = 36)
	private String familyId;

	/** 속한 로그인 세션. 세션 없이 발급한 토큰(골격)이면 null */
	@Column(name = "login_session_id")
	private Long loginSessionId;

	@Column(name = "token_hash", nullable = false, length = 64)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "used_at")
	private LocalDateTime usedAt;

	@Column(name = "revoked_at")
	private LocalDateTime revokedAt;

	public RefreshToken(Long userId, String familyId, String tokenHash, LocalDateTime expiresAt) {
		this(userId, familyId, null, tokenHash, expiresAt);
	}

	public RefreshToken(Long userId, String familyId, Long loginSessionId, String tokenHash,
		LocalDateTime expiresAt) {
		this.userId = userId;
		this.familyId = familyId;
		this.loginSessionId = loginSessionId;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	public boolean isUsed() {
		return usedAt != null;
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}

	public boolean isExpired(LocalDateTime now) {
		return !expiresAt.isAfter(now);
	}

	public void markUsed(LocalDateTime now) {
		this.usedAt = now;
	}

	public void revoke(LocalDateTime now) {
		if (revokedAt == null) {
			this.revokedAt = now;
		}
	}
}
