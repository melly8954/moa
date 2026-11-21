package com.moa.api.application.v1.user;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.dto.role.GrantBlock;
import com.moa.api.dto.role.RoleUserDto;
import com.moa.api.dto.user.CreatedUserDto;
import com.moa.api.dto.user.UserAdminDto;
import com.moa.api.dto.user.UserAdminRowDto;
import com.moa.api.service.role.AccessRoleService;
import com.moa.api.service.role.RoleUserService;
import com.moa.api.service.user.UserAdminService;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.Role;
import com.moa.common.security.permission.PermissionGuard;
import com.moa.common.security.permission.PermissionRegistry;
import com.moa.common.security.token.AuthTokenService;

import lombok.RequiredArgsConstructor;

/**
 * 사용자 관리 (선택 헬퍼 user-admin). 잠그기·비밀번호 초기화·지우기는 그 사용자를 모든 기기에서 로그아웃시킨다
 * (리프레시 토큰 폐기. 이미 받은 액세스 토큰은 만료될 때까지 쓸 수 있다).
 *
 * moa (specs/002-admin-users FR-005·FR-007, research R9·R10): 바꾸는 일 전에 다루기 범위를 본다. 최고 관리자가 아니면
 * 최고 관리자 계정은 못 다루고, 관리자 계정은 그 권한이 모두 내 권한 안에 있을 때만 다룬다. 계정 종류를 회원으로 바꾸면
 * 역할을 모두 빼고 로그아웃시킨다. 최고 관리자가 0명이 되는 일(회원으로 바꾸기·잠그기·지우기)은 B001이다
 */
@Component
@RequiredArgsConstructor
public class UserAdminApplication {

	private final UserAdminService userAdminService;
	private final AuthTokenService authTokenService;
	private final RoleUserService roleUserService;
	private final AccessRoleService accessRoleService;
	private final PermissionGuard permissionGuard;
	private final PermissionRegistry permissionRegistry;

	@Transactional(readOnly = true)
	public Page<UserAdminRowDto> search(AuthPrincipal actor, String keyword, Role role, Boolean locked,
		Pageable pageable) {
		Page<UserAdminDto> users = userAdminService.search(keyword, role, locked, pageable);
		List<RowFacts> facts = rowFacts(actor, users.getContent());
		Map<Long, RowFacts> byId = new HashMap<>();
		facts.forEach(fact -> byId.put(fact.user().id(), fact));
		return users.map(user -> byId.get(user.id()).row());
	}

	@Transactional(readOnly = true)
	public UserAdminRowDto get(AuthPrincipal actor, Long id) {
		return rowFacts(actor, List.of(userAdminService.get(id))).getFirst().row();
	}

	@Transactional
	public CreatedUserDto create(AuthPrincipal actor, String loginId, String name, Role role) {
		return userAdminService.create(actor, loginId, name, role);
	}

	@Transactional
	public UserAdminDto rename(AuthPrincipal actor, Long id, String name) {
		requireManageable(actor, id);
		return userAdminService.rename(actor, id, name);
	}

	/** moa: ADMIN → USER면 역할을 모두 빼고, 계정 종류가 바뀌면 로그아웃시킨다 (토큰에 계정 종류가 들어 있다) */
	@Transactional
	public UserAdminDto changeRole(AuthPrincipal actor, Long id, Role role) {
		UserAdminDto before = requireManageable(actor, id);
		if (before.role() == Role.ADMIN && role != Role.ADMIN) {
			accessRoleService.requireSuperAdminRemains(List.of(id));
		}
		UserAdminDto changed = userAdminService.changeRole(actor, id, role);
		if (before.role() != role) {
			if (role != Role.ADMIN) {
				accessRoleService.removeAllRolesOf(id);
			}
			authTokenService.revokeAll(id);
		}
		return changed;
	}

	@Transactional
	public void lock(AuthPrincipal actor, Long id) {
		requireManageable(actor, id);
		accessRoleService.requireSuperAdminRemains(List.of(id));
		userAdminService.lock(actor, id);
		authTokenService.revokeAll(id);
	}

	@Transactional
	public void unlock(AuthPrincipal actor, Long id) {
		requireManageable(actor, id);
		userAdminService.unlock(actor, id);
	}

	@Transactional
	public String resetPassword(AuthPrincipal actor, Long id) {
		requireManageable(actor, id);
		String temporary = userAdminService.resetPassword(actor, id);
		authTokenService.revokeAll(id);
		return temporary;
	}

	/** moa: 지운 계정의 역할도 뺀다 (최고 관리자 수를 셀 때 지운 계정이 남지 않게) */
	@Transactional
	public void delete(AuthPrincipal actor, Long id) {
		requireManageable(actor, id);
		accessRoleService.requireSuperAdminRemains(List.of(id));
		userAdminService.delete(actor, id);
		accessRoleService.removeAllRolesOf(id);
		authTokenService.revokeAll(id);
	}

	/** moa: 다루기 범위 (R9). 없는 계정 R001, 다룰 수 없으면 A005 */
	private UserAdminDto requireManageable(AuthPrincipal actor, Long id) {
		UserAdminDto user = userAdminService.get(id);
		userAdminService.requireNotProtected(rowFacts(actor, List.of(user)).getFirst().row().protection());
		return user;
	}

	/** moa: 한 쪽의 계정에 역할·최고 관리자·다루기 범위를 붙인다. 쿼리를 계정마다 하지 않고 한 번에 한다 (R10) */
	private List<RowFacts> rowFacts(AuthPrincipal actor, List<UserAdminDto> users) {
		List<Long> ids = users.stream().map(UserAdminDto::id).toList();
		Map<Long, List<RoleUserDto.RoleRef>> roles = roleUserService.accessRolesOf(ids);
		Set<Long> superAdmins = accessRoleService.findSuperAdminIds(ids);
		boolean actorHasAll = permissionGuard.hasAll(actor);
		Set<String> mine = permissionGuard.permissions(actor);
		Map<Long, Set<String>> permissions = actorHasAll ? Map.of() : accessRoleService.permissionsOf(ids);
		return users.stream().map(user -> {
			GrantBlock protection = null;
			if (!actorHasAll && user.role() == Role.ADMIN) {
				if (superAdmins.contains(user.id())) {
					protection = GrantBlock.SUPER_ADMIN_ONLY;
				} else if (!permissions.getOrDefault(user.id(), Set.of()).stream()
					.filter(permissionRegistry::contains)
					.allMatch(mine::contains)) {
					protection = GrantBlock.WIDER_PERMISSIONS;
				}
			}
			return new RowFacts(user, new UserAdminRowDto(user, roles.getOrDefault(user.id(), List.of()),
				superAdmins.contains(user.id()), protection));
		}).toList();
	}

	private record RowFacts(UserAdminDto user, UserAdminRowDto row) {
	}
}
