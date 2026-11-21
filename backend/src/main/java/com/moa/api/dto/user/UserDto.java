package com.moa.api.dto.user;

import java.util.Set;

import com.moa.api.entity.user.User;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.Role;

/**
 * @param hasPassword moa: 비밀번호가 있는지. 없으면 소셜 계정 (specs/010-social-login US2-AC3)
 */
public record UserDto(Long id, String loginId, String name, Role role, boolean passwordChangeRequired,
	boolean hasPassword) {

	public static UserDto from(User user) {
		return new UserDto(user.getId(), user.getLoginId(), user.getName(), user.getRole(),
			user.isPasswordChangeRequired(), user.hasPassword());
	}

	public AuthPrincipal toPrincipal() {
		return new AuthPrincipal(id, Set.of(role.name()));
	}
}
