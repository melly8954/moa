package com.moa.api.dto;

import java.time.Instant;

/**
 * 보낸 인증 메일.
 *
 * @param email 보낸 주소
 * @param expiresAt 링크 만료 시각
 * @param resendAvailableAt 다시 보낼 수 있는 시각
 */
public record EmailVerificationDto(String email, Instant expiresAt, Instant resendAvailableAt) {
}
