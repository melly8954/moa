package com.moa.api.application.v1.role;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.application.v1.role.command.CreateAccessRoleCommand;
import com.moa.api.application.v1.role.command.ModifyAccessRoleCommand;
import com.moa.api.dto.role.AccessRoleDto;
import com.moa.api.service.role.AccessRoleService;
import com.moa.api.service.user.UserService;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.permission.PermissionDefinition;
import com.moa.common.security.permission.PermissionRegistry;

import lombok.RequiredArgsConstructor;

/**
 * 역할·권한 관리 (선택 헬퍼 rbac). actor는 요청한 사용자다 (권한 상승 검사에 쓴다).
 */
@Component
@RequiredArgsConstructor
public class AccessRoleApplication {

	private final AccessRoleService accessRoleService;
	private final UserService userService;
	private final PermissionRegistry permissionRegistry;

	/** 코드에 등록된 권한 전부 (역할에 묶을 수 있는 것) */
	public List<PermissionDefinition> getPermissions() {
		return permissionRegistry.all();
	}

	@Transactional(readOnly = true)
	public List<AccessRoleDto> getRoles() {
		return accessRoleService.getAll();
	}

	@Transactional(readOnly = true)
	public AccessRoleDto getRole(Long id) {
		return accessRoleService.get(id);
	}

	@Transactional
	public AccessRoleDto create(AuthPrincipal actor, CreateAccessRoleCommand command) {
		return accessRoleService.create(actor, command.code(), command.name(), command.description(),
			command.permissions());
	}

	@Transactional
	public AccessRoleDto modify(AuthPrincipal actor, ModifyAccessRoleCommand command) {
		return accessRoleService.modify(actor, command.id(), command.name(), command.description(),
			command.permissions());
	}

	@Transactional
	public void delete(AuthPrincipal actor, Long id) {
		accessRoleService.delete(actor, id);
	}

	/** 없는 사용자면 R001 */
	@Transactional(readOnly = true)
	public List<AccessRoleDto> getRolesOfUser(Long userId) {
		userService.find(userId);
		return accessRoleService.getRolesOfUser(userId);
	}

	/** 없는 사용자·역할이면 R001 */
	@Transactional
	public void replaceRolesOfUser(AuthPrincipal actor, Long userId, List<Long> roleIds) {
		userService.find(userId);
		accessRoleService.replaceRolesOfUser(actor, userId, roleIds);
	}
}
