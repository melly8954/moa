package com.moa.api.application.v1.user;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.dto.user.MeDto;
import com.moa.api.dto.user.UserDto;
import com.moa.api.service.role.AccessRoleService;
import com.moa.api.service.user.MemberActivityPolicy;
import com.moa.api.service.user.ProfileService;
import com.moa.api.service.user.UserService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserApplication {

	private final UserService userService;
	private final AccessRoleService accessRoleService;
	private final ProfileService profileService;
	private final MemberActivityPolicy memberActivityPolicy;

	/** moa: 아바타와 마이페이지 허용을 붙인다 (specs/009-mypage FR-014·FR-016) */
	@Transactional(readOnly = true)
	public MeDto getMe(Long userId) {
		UserDto user = userService.find(userId);
		return new MeDto(user, profileService.getAvatar(userId), !memberActivityPolicy.isManagerAccount(userId));
	}

	/** moa: 최고 관리자가 있는가 (specs/002-admin-users FR-001). 키트는 "ADMIN이 있는가"였다 */
	@Transactional(readOnly = true)
	public boolean hasAdmin() {
		return accessRoleService.hasSuperAdmin();
	}

	/**
	 * 첫 관리자 (AdminBootstrapRunner). 최고 관리자가 이미 있으면 만들지 않는다. 만들었으면 true.
	 * moa: 만든 계정을 최고 관리자 역할에 넣는다 (specs/003-admin-roles FR-010, research R20)
	 */
	@Transactional
	public boolean bootstrapAdmin(String loginId, String temporaryPassword, String name) {
		if (accessRoleService.hasSuperAdmin()) {
			return false;
		}
		accessRoleService.addSuperAdmin(userService.bootstrapAdmin(loginId, temporaryPassword, name));
		return true;
	}

	@Transactional
	public void changePassword(Long userId, String currentPassword, String newPassword) {
		userService.changePassword(userId, currentPassword, newPassword);
	}
}
