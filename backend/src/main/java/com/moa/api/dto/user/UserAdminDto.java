package com.moa.api.dto.user;

import java.time.LocalDateTime;

import com.moa.api.entity.user.User;
import com.moa.common.security.Role;

/**
 * 관리 화면의 사용자 (선택 헬퍼 user-admin).
 *
 * @param hasPassword 비밀번호로 로그인하는 계정인지 (소셜 계정은 false, 비밀번호 초기화를 할 수 없다)
 */
public record UserAdminDto(Long id, String loginId, String name, Role role, boolean locked, LocalDateTime lockedAt,
	boolean passwordChangeRequired, boolean hasPassword, LocalDateTime createdAt) {

	public static UserAdminDto from(User user) {
		return new UserAdminDto(user.getId(), user.getLoginId(), user.getName(), user.getRole(), user.isLocked(),
			user.getLockedAt(), user.isPasswordChangeRequired(), user.hasPassword(), user.getCreatedAt());
	}
}
