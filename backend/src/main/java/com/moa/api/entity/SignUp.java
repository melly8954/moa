package com.moa.api.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.moa.common.entity.BaseTimeEntity;
import com.moa.common.entity.HardDelete;
import com.moa.common.oauth.OAuthProvider;

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
 * 가입 진행. 회원을 만들기 전까지 확인된 값(계정 이메일, 비밀번호, 제공자 계정, 생년월일, 인증된 휴대폰 번호)을 둔다.
 * 브라우저는 토큰 원문을 쿠키로 가지고, 서버는 해시만 둔다.
 */
@Getter
@Entity
@Table(name = "sign_ups")
@HardDelete(reason = "회원이 생기거나 가입을 취소·만료하면 쓸 일이 없는 일회성 값이다")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SignUp extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "token_hash", nullable = false, length = 64)
	private String tokenHash;

	@Column(name = "email")
	private String email;

	@Column(name = "password_hash", length = 100)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider", length = 30)
	private OAuthProvider provider;

	@Column(name = "provider_user_id")
	private String providerUserId;

	@Column(name = "birth_date")
	private LocalDate birthDate;

	@Column(name = "phone_encrypted")
	private String phoneEncrypted;

	@Column(name = "phone_hmac", length = 64)
	private String phoneHmac;

	@Column(name = "phone_verified_at")
	private LocalDateTime phoneVerifiedAt;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;
}
