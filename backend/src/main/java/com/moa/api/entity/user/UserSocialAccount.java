package com.moa.api.entity.user;

import java.time.LocalDateTime;

import com.moa.common.entity.HardDelete;
import com.moa.common.security.oauth.SocialProvider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 외부(소셜) 계정 연결 (specs/010-social-login data-model). 회원 하나에 제공자마다 하나.
 * 이메일·제공자 이름은 연결 때 값 그대로 두고 바꾸지 않는다 (Edge Cases).
 */
@Getter
@Entity
@Table(name = "user_social_accounts")
@HardDelete(reason = "연결 해제(FR-006)는 행을 지운다. 이력이 아니다")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSocialAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SocialProvider provider;

	@Column(nullable = false)
	private String subject;

	@Column(nullable = false)
	private String email;

	@Column(name = "display_name", length = 100)
	private String displayName;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public UserSocialAccount(Long userId, SocialProvider provider, String subject, String email, String displayName,
		LocalDateTime createdAt) {
		this.userId = userId;
		this.provider = provider;
		this.subject = subject;
		this.email = email;
		this.displayName = displayName;
		this.createdAt = createdAt;
	}
}
