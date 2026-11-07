package com.moa.api.application.v1.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.dto.user.CreatedUserDto;
import com.moa.api.dto.user.UserAdminDto;
import com.moa.api.service.user.UserAdminService;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.Role;
import com.moa.common.security.token.AuthTokenService;

import lombok.RequiredArgsConstructor;

/**
 * 사용자 관리 (선택 헬퍼 user-admin). 잠그기·비밀번호 초기화·지우기는 그 사용자를 모든 기기에서 로그아웃시킨다
 * (리프레시 토큰 폐기. 이미 받은 액세스 토큰은 만료될 때까지 쓸 수 있다).
 */
@Component
@RequiredArgsConstructor
public class UserAdminApplication {

	private final UserAdminService userAdminService;
	private final AuthTokenService authTokenService;

	@Transactional(readOnly = true)
	public Page<UserAdminDto> search(String keyword, Role role, Boolean locked, Pageable pageable) {
		return userAdminService.search(keyword, role, locked, pageable);
	}

	@Transactional(readOnly = true)
	public UserAdminDto get(Long id) {
		return userAdminService.get(id);
	}

	@Transactional
	public CreatedUserDto create(AuthPrincipal actor, String loginId, String name, Role role) {
		return userAdminService.create(actor, loginId, name, role);
	}

	@Transactional
	public UserAdminDto rename(AuthPrincipal actor, Long id, String name) {
		return userAdminService.rename(actor, id, name);
	}

	@Transactional
	public UserAdminDto changeRole(AuthPrincipal actor, Long id, Role role) {
		return userAdminService.changeRole(actor, id, role);
	}

	@Transactional
	public void lock(AuthPrincipal actor, Long id) {
		userAdminService.lock(actor, id);
		authTokenService.revokeAll(id);
	}

	@Transactional
	public void unlock(AuthPrincipal actor, Long id) {
		userAdminService.unlock(actor, id);
	}

	@Transactional
	public String resetPassword(AuthPrincipal actor, Long id) {
		String temporary = userAdminService.resetPassword(actor, id);
		authTokenService.revokeAll(id);
		return temporary;
	}

	@Transactional
	public void delete(AuthPrincipal actor, Long id) {
		userAdminService.delete(actor, id);
		authTokenService.revokeAll(id);
	}
}
