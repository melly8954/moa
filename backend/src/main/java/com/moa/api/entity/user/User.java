package com.moa.api.entity.user;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLRestriction;

import com.moa.common.entity.SoftDeleteEntity;
import com.moa.common.security.Role;

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
 * 사용자. 골격의 회원·로그인 기능이 쓴다. 프로젝트에 맞춰 칸을 더한다 (이메일, 연락처 등).
 *
 * moa 018 (specs/018-member-withdrawal research R8): 지운 회원과 함께 탈퇴(유예 중)한 회원도 기본 조회에서 숨긴다. 유예 중의 보관은
 * 복구 목적으로만 한다(docs/privacy.md). 로그인·복구·파기만 UserRepository의 native 쿼리로 예외로 읽는다.
 */
@Getter
@Entity
@Table(name = "tb_user")
@SQLRestriction(User.ACTIVE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends SoftDeleteEntity {

	/** moa 018: 기본 조회에 나오는 회원 */
	public static final String ACTIVE = NOT_DELETED + " AND withdrawn_at IS NULL";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "login_id", nullable = false, length = 50)
	private String loginId;

	/** {bcrypt}... 형식의 해시. null이면 비밀번호로 로그인할 수 없다 (시스템 계정) */
	@Column(length = 100)
	private String password;

	@Column(nullable = false, length = 50)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	/** 잠근 시각. 값이 있으면 로그인·토큰 재발급이 막힌다 */
	@Column(name = "locked_at")
	private LocalDateTime lockedAt;

	/** 임시 비밀번호를 받아 다음 로그인 때 바꿔야 한다 */
	@Column(name = "password_change_required", nullable = false)
	private boolean passwordChangeRequired;

	/** moa 018: 스스로 탈퇴한 시각. 복구하면 비운다. 파기해도 남긴다 */
	@Column(name = "withdrawn_at")
	private LocalDateTime withdrawnAt;

	/** moa 018: 개인정보를 파기한 시각. 파기는 native UPDATE가 채운다 */
	@Column(name = "purged_at", insertable = false, updatable = false)
	private LocalDateTime purgedAt;

	public User(String loginId, String encodedPassword, String name, Role role) {
		this.loginId = loginId;
		this.password = encodedPassword;
		this.name = name;
		this.role = role;
	}

	/** 비밀번호 로그인을 할 수 있는지. 소셜 계정·시스템 계정은 비밀번호가 없다 */
	public boolean hasPassword() {
		return password != null;
	}

	public boolean isLocked() {
		return lockedAt != null;
	}

	public void lock(LocalDateTime now) {
		if (lockedAt == null) {
			lockedAt = now;
		}
	}

	public void unlock() {
		lockedAt = null;
	}

	/** 본인이 바꾼다. 변경 요구가 풀린다 */
	public void changePassword(String encodedPassword) {
		this.password = encodedPassword;
		this.passwordChangeRequired = false;
	}

	/** 관리자가 임시 비밀번호로 바꾼다. 다음 로그인 때 바꿔야 한다 */
	public void resetPassword(String encodedTemporaryPassword) {
		this.password = encodedTemporaryPassword;
		this.passwordChangeRequired = true;
	}

	public void changeRole(Role role) {
		this.role = role;
	}

	public void rename(String name) {
		this.name = name;
	}

	public boolean isWithdrawn() {
		return withdrawnAt != null;
	}

	/** moa 018: 탈퇴. 이미 탈퇴했으면 처음 시각을 그대로 둔다 */
	public void withdraw(LocalDateTime now) {
		if (withdrawnAt == null) {
			withdrawnAt = now;
		}
	}

	/** moa 018: 유예 기간 안의 복구 */
	public void restore() {
		withdrawnAt = null;
	}
}
