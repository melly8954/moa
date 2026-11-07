package com.moa.api.dto.user;

import java.util.Set;

import com.moa.api.entity.user.User;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.Role;

public record UserDto(Long id, String loginId, String name, Role role, boolean passwordChangeRequired) {

	public static UserDto from(User user) {
		return new UserDto(user.getId(), user.getLoginId(), user.getName(), user.getRole(),
			user.isPasswordChangeRequired());
	}

	public AuthPrincipal toPrincipal() {
		return new AuthPrincipal(id, Set.of(role.name()));
	}
}
