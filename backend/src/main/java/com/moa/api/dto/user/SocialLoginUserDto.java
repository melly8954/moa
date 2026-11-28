package com.moa.api.dto.user;

import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;

/**
 * 소셜 계정이 연결된 회원 (specs/010-social-login FR-003). 잠긴 회원은 로그인시키지 않는다.
 * moa 018: 탈퇴(유예 중·파기 대기)한 회원도 나온다. 로그인시키지 않고 복구 안내나 파기 대기 안내로 보낸다
 * (specs/018-member-withdrawal research R5)
 *
 * @param withdrawnAt 탈퇴한 시각. 탈퇴하지 않았으면 null
 */
public record SocialLoginUserDto(UserDto user, boolean locked, @Nullable LocalDateTime withdrawnAt) {
}
