package com.moa.api.application.v1.user;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.dto.user.UserDto;
import com.moa.api.service.user.UserService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserApplication {

	private final UserService userService;

	@Transactional(readOnly = true)
	public UserDto getMe(Long userId) {
		return userService.find(userId);
	}

	@Transactional(readOnly = true)
	public boolean hasAdmin() {
		return userService.hasAdmin();
	}

	/** 첫 관리자 (AdminBootstrapRunner). 만들었으면 true */
	@Transactional
	public boolean bootstrapAdmin(String loginId, String temporaryPassword, String name) {
		return userService.bootstrapAdmin(loginId, temporaryPassword, name);
	}

	@Transactional
	public void changePassword(Long userId, String currentPassword, String newPassword) {
		userService.changePassword(userId, currentPassword, newPassword);
	}
}
