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

	/**
	 * 이메일 인증을 마친 이메일 가입.
	 *
	 * @param expiresAt 가입 진행 만료 시각. 인증 메일을 요청한 때부터 잰다
	 */
	public static SignUp startWithEmail(String tokenHash, String email, String passwordHash,
		LocalDateTime expiresAt) {
		SignUp signUp = new SignUp();
		signUp.tokenHash = tokenHash;
		signUp.email = email;
		signUp.passwordHash = passwordHash;
		signUp.expiresAt = expiresAt;
		return signUp;
	}

	/**
	 * 구글·카카오 가입.
	 *
	 * @param email 제공자가 인증된 이메일로 알려 준 이메일. 주지 않았거나 인증되지 않았으면 null이고 이메일 인증으로 정한다
	 * @param expiresAt 가입 진행 만료 시각. 제공자 인증에서 돌아온 때부터 잰다
	 */
	public static SignUp startWithProvider(String tokenHash, OAuthProvider provider, String providerUserId,
		String email, LocalDateTime expiresAt) {
		SignUp signUp = new SignUp();
		signUp.tokenHash = tokenHash;
		signUp.provider = provider;
		signUp.providerUserId = providerUserId;
		signUp.email = email;
		signUp.expiresAt = expiresAt;
		return signUp;
	}

	public boolean isExpired(LocalDateTime now) {
		return !expiresAt.isAfter(now);
	}

	/** 제공자가 이메일을 주지 않았거나 인증되지 않은 이메일을 줘서 이메일 인증을 기다리는 가입 */
	public boolean awaitsEmail() {
		return email == null && provider != null;
	}

	public boolean isPhoneVerified() {
		return phoneVerifiedAt != null;
	}

	/** 이메일 인증으로 계정 이메일과 비밀번호를 정한다. 다른 브라우저에서 이어 가도록 토큰을 새로 바꾼다 */
	public void confirmEmail(String email, String passwordHash, String newTokenHash) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.tokenHash = newTokenHash;
	}

	public void verifyPhone(LocalDate birthDate, String phoneEncrypted, String phoneHmac, LocalDateTime now) {
		this.birthDate = birthDate;
		this.phoneEncrypted = phoneEncrypted;
		this.phoneHmac = phoneHmac;
		this.phoneVerifiedAt = now;
	}
}
