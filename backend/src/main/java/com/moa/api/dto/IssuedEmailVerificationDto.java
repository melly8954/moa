package com.moa.api.dto;

import java.time.Instant;

/**
 * 저장했지만 아직 보내지 않은 인증 메일. 발송은 트랜잭션 밖에서 한다.
 *
 * @param id 이메일 인증 ID. 발송에 실패하면 지운다
 * @param email 보낼 주소
 * @param rawToken 링크 토큰 원문. 메일 본문에만 넣는다
 * @param expiresAt 링크 만료 시각
 * @param resendAvailableAt 다시 보낼 수 있는 시각
 */
public record IssuedEmailVerificationDto(Long id, String email, String rawToken, Instant expiresAt,
	Instant resendAvailableAt) {
}
