package com.moa.api.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.SQLRestriction;

import com.moa.common.entity.SoftDeleteEntity;

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
 * 회원. 휴대폰 번호는 원문을 암호화해 두고(phoneEncrypted), 중복 검사는 HMAC 값(phoneHmac)으로 한다.
 */
@Getter
@Entity
@Table(name = "members")
@SQLRestriction(SoftDeleteEntity.NOT_DELETED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends SoftDeleteEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "email")
	private String email;

	@Column(name = "password_hash", length = 100)
	private String passwordHash;

	@Column(name = "nickname", length = 12)
	private String nickname;

	@Column(name = "birth_date")
	private LocalDate birthDate;

	@Column(name = "phone_encrypted")
	private String phoneEncrypted;

	@Column(name = "phone_hmac", length = 64)
	private String phoneHmac;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 30)
	private MemberRole role;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private MemberStatus status;

	@Column(name = "terms_agreed_at")
	private LocalDateTime termsAgreedAt;

	@Column(name = "privacy_agreed_at")
	private LocalDateTime privacyAgreedAt;

	/**
	 * 가입을 마친 ACTIVE 회원.
	 *
	 * @param passwordHash 비밀번호가 없으면(구글·카카오 가입) null
	 */
	public static Member createActive(String email, String passwordHash, String nickname, LocalDate birthDate,
		String phoneEncrypted, String phoneHmac, LocalDateTime agreedAt) {
		Member member = new Member();
		member.email = email;
		member.passwordHash = passwordHash;
		member.nickname = nickname;
		member.birthDate = birthDate;
		member.phoneEncrypted = phoneEncrypted;
		member.phoneHmac = phoneHmac;
		member.role = MemberRole.MEMBER;
		member.status = MemberStatus.ACTIVE;
		member.termsAgreedAt = agreedAt;
		member.privacyAgreedAt = agreedAt;
		return member;
	}

	public boolean isActive() {
		return status == MemberStatus.ACTIVE;
	}

	public boolean hasPassword() {
		return passwordHash != null;
	}

	/** 이메일 인증 연결로 비밀번호 로그인 수단을 더한다 */
	public void addPassword(String passwordHash) {
		this.passwordHash = passwordHash;
	}
}
