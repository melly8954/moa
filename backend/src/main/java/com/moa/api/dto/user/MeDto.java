package com.moa.api.dto.user;

/**
 * moa: 내 정보 (GET /api/v1/users/me). UserDto는 로그인·토큰 발급도 쓰므로 고치지 않고 이 응답에만 붙는 값을 따로 담는다
 * (specs/009-mypage contracts/users-me-and-screens.md).
 *
 * @param avatar 계정 바 아바타 키. 프로필이 없거나 목록에서 빠진 키면 default (FR-014)
 * @param mypageAllowed 관리자 계정(ADMIN)이 아니면 참 (FR-016)
 */
public record MeDto(UserDto user, String avatar, boolean mypageAllowed) {
}
