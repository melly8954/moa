package com.moa.api.dto;

import java.time.Instant;

/**
 * 보낸 문자 인증 번호.
 *
 * @param expiresAt 인증 번호 만료 시각
 * @param resendAvailableAt 다시 받을 수 있는 시각
 */
public record PhoneVerificationDto(Instant expiresAt, Instant resendAvailableAt) {
}
