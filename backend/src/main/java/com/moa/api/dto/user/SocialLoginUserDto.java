package com.moa.api.dto.user;

/**
 * 소셜 계정이 연결된 회원 (specs/010-social-login FR-003). 잠긴 회원은 로그인시키지 않는다.
 */
public record SocialLoginUserDto(UserDto user, boolean locked) {
}
