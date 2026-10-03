package com.moa.api.dto;

import java.time.Instant;

import com.moa.common.oauth.OAuthProvider;

/**
 * 가입 진행 상태.
 *
 * @param email 계정 이메일. 카카오가 이메일을 주지 않아 아직 정해지지 않았으면 null
 * @param provider 구글·카카오 가입이면 제공자, 이메일 가입이면 null
 * @param phoneVerified 휴대폰 인증을 마쳤는지
 * @param phoneConflict 인증한 휴대폰 번호가 기존 계정과 겹치면 그 안내 정보, 아니면 null
 * @param expiresAt 가입 진행 만료 시각
 */
public record SignUpDto(String email, OAuthProvider provider, boolean phoneVerified, PhoneConflict phoneConflict,
	Instant expiresAt) {

	/**
	 * 휴대폰 번호 중복 안내.
	 *
	 * @param accountStatus 겹친 계정의 상태 (ACTIVE, SUSPENDED, WITHDRAWN, PURGED)
	 * @param maskedEmail 겹친 계정 이메일을 일부 가린 값. ACTIVE·SUSPENDED일 때만 있다
	 */
	public record PhoneConflict(String accountStatus, String maskedEmail) {
	}
}
