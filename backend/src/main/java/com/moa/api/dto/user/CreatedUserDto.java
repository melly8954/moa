package com.moa.api.dto.user;

/**
 * 관리자가 만든 사용자와 임시 비밀번호 (선택 헬퍼 user-admin). 임시 비밀번호는 이 응답에서만 보인다.
 */
public record CreatedUserDto(UserAdminDto user, String temporaryPassword) {
}
