package com.moa.api.entity.user;

import java.time.LocalDateTime;

import com.moa.common.entity.HardDelete;
import com.moa.common.terms.TermsType;

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
 * 약관 동의 기록 (specs/010-social-login FR-013). 가입 때 종류마다 한 행. 고치지 않는다.
 */
@Getter
@Entity
@Table(name = "user_terms_agreements")
@HardDelete(reason = "동의 기록은 고치거나 지우지 않는다. 탈퇴 때 처리는 회원 탈퇴 스펙이 정한다")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTermsAgreement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "terms_type", nullable = false, length = 20)
	private TermsType termsType;

	@Column(name = "terms_version", nullable = false, length = 20)
	private String termsVersion;

	@Column(name = "agreed_at", nullable = false, updatable = false)
	private LocalDateTime agreedAt;

	public UserTermsAgreement(Long userId, TermsType termsType, String termsVersion, LocalDateTime agreedAt) {
		this.userId = userId;
		this.termsType = termsType;
		this.termsVersion = termsVersion;
		this.agreedAt = agreedAt;
	}
}
