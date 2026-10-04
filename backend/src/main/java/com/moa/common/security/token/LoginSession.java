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
 * 로그인 세션. 기기·브라우저 하나의 로그인 하나다. 액세스 토큰의 sid 클레임이 id이고,
 * 리프레시 토큰은 세션에 속한다. 끊으면(revokedAt) 그 세션의 토큰으로 하는 요청을 거절한다.
 */
@Getter
@Entity
@Table(name = "login_sessions")
@HardDelete(reason = "끊기거나 리프레시 수명이 지난 세션은 예약 작업이 지운다")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginSession extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "revoked_at")
	private LocalDateTime revokedAt;

	/** 끊은 사유 (로그아웃, 비밀번호 변경·재설정, 제재, 탈퇴). 끊지 않았으면 null */
	@Column(name = "revoke_reason", length = 30)
	private String revokeReason;

	public LoginSession(Long memberId) {
		this.memberId = memberId;
	}
}
