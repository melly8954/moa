package com.moa.api.application.v1.role;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.application.v1.role.command.CreateAccessRoleCommand;
import com.moa.api.application.v1.role.command.ModifyAccessRoleCommand;
import com.moa.api.dto.menu.MenuAudience;
import com.moa.api.dto.menu.MenuDto;
import com.moa.api.dto.role.AccessRoleDto;
import com.moa.api.dto.role.PermissionTreeDto;
import com.moa.api.dto.role.RoleMemberDto;
import com.moa.api.screen.ScreenRegistry;
import com.moa.api.service.menu.MenuService;
import com.moa.api.service.role.AccessRoleService;
import com.moa.api.service.role.RoleUserService;
import com.moa.api.service.user.UserService;
import com.moa.common.audit.AuditProperties;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.permission.PermissionDefinition;
import com.moa.common.security.permission.PermissionRegistry;

import lombok.RequiredArgsConstructor;

/**
 * 역할·권한 관리 (선택 헬퍼 rbac). actor는 요청한 사용자다 (권한 상승 검사에 쓴다).
 * moa: 권한 트리, 사용자별 역할, 구성원 (specs/003-admin-roles FR-004~FR-008). 권한 트리는 menu 헬퍼의 메뉴와 연결 화면을 읽는다.
 */
@Component
@RequiredArgsConstructor
public class AccessRoleApplication {

	private final AccessRoleService accessRoleService;
	private final RoleUserService roleUserService;
	private final UserService userService;
	private final PermissionRegistry permissionRegistry;
	private final MenuService menuService;
	private final ScreenRegistry screenRegistry;
	private final AuditProperties auditProperties;

	/** 코드에 등록된 권한 전부 (역할에 묶을 수 있는 것) */
	public List<PermissionDefinition> getPermissions() {
		return permissionRegistry.all();
	}

	/**
	 * 역할 창에서 권한을 고르는 트리 (moa, FR-004). 관리 메뉴 트리(꺼진 메뉴 포함, 없는 연결 화면을 가리키는 메뉴는 뺌)와
	 * 어느 관리 메뉴도 가리키지 않는 관리 연결 화면.
	 */
	@Transactional(readOnly = true)
	public PermissionTreeDto getPermissionTree() {
		List<MenuDto> adminMenus = menuService.getTree().stream()
			.filter(menu -> menu.audience() == MenuAudience.ADMIN)
			.toList();
		Set<String> listed = new HashSet<>();
		adminMenus.forEach(menu -> {
			listed.add(menu.screenKey());
			menu.children().forEach(child -> listed.add(child.screenKey()));
		});
		List<PermissionTreeDto.Node> nodes = adminMenus.stream()
			.map(this::node)
			.filter(Objects::nonNull)
			.toList();
		List<PermissionTreeDto.Unlisted> unlisted = screenRegistry.of(MenuAudience.ADMIN).stream()
			.filter(screen -> !listed.contains(screen.key()))
			.map(screen -> new PermissionTreeDto.Unlisted(screen.key(), screen.name(), screen.permission(),
				permissionName(screen.permission())))
			.toList();
		return new PermissionTreeDto(nodes, unlisted);
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

	/** 없는 사용자·역할이면 R001. moa: 대상이 ADMIN이면 B001 (specs/003-admin-roles Edge Cases) */
	@Transactional
	public void replaceRolesOfUser(AuthPrincipal actor, Long userId, List<Long> roleIds) {
		roleUserService.requireNotAdmin(userService.find(userId).role());
		accessRoleService.replaceRolesOfUser(actor, userId, roleIds);
	}

	/** 여러 역할을 한꺼번에 지운다 (moa, FR-005). 없는 역할 R001, 다룰 수 없는 역할 A005 (아무것도 지우지 않는다) */
	@Transactional
	public void deleteAll(AuthPrincipal actor, List<Long> ids) {
		accessRoleService.deleteAll(actor, ids);
	}

	/** 역할 드로어 "구성원" 탭 (moa, FR-006). 없는 역할 R001 */
	@Transactional(readOnly = true)
	public Page<RoleMemberDto> getMembers(Long roleId, String keyword, Pageable pageable) {
		return roleUserService.getMembers(auditProperties.systemActorId(), roleId, keyword, pageable);
	}

	/** 없는 역할·사용자 R001, 다룰 수 없는 역할 A005, ADMIN이 섞이면 B001 (moa, FR-006). 하나라도 틀리면 아무도 더하지 않는다 */
	@Transactional
	public void addMembers(AuthPrincipal actor, Long roleId, List<Long> userIds) {
		accessRoleService.requireExists(roleId);
		roleUserService.requireTargets(auditProperties.systemActorId(), userIds);
		accessRoleService.addMembers(actor, roleId, userIds);
	}

	/** 없는 역할·사용자 R001, 다룰 수 없는 역할 A005, ADMIN이 섞이면 B001 (moa, FR-006). 하나라도 틀리면 아무도 빼지 않는다 */
	@Transactional
	public void removeMembers(AuthPrincipal actor, Long roleId, List<Long> userIds) {
		accessRoleService.requireExists(roleId);
		roleUserService.requireTargets(auditProperties.systemActorId(), userIds);
		accessRoleService.removeMembers(actor, roleId, userIds);
	}

	/** 화면 메뉴는 연결 화면의 권한, 묶음 메뉴는 하위 메뉴를 담는다. 없는 연결 화면과 하위 메뉴 없는 묶음은 null */
	private PermissionTreeDto.Node node(MenuDto menu) {
		if (menu.screenKey() == null) {
			List<PermissionTreeDto.Node> children = menu.children().stream()
				.map(this::node)
				.filter(Objects::nonNull)
				.toList();
			return children.isEmpty() ? null
				: new PermissionTreeDto.Node(menu.id(), menu.name(), menu.enabled(), null, null, children);
		}
		if (menu.screen() == null) {
			return null;
		}
		String permission = menu.screen().permission();
		return new PermissionTreeDto.Node(menu.id(), menu.name(), menu.enabled(), permission,
			permissionName(permission), List.of());
	}

	private String permissionName(String key) {
		return permissionRegistry.find(key).map(PermissionDefinition::name).orElse(null);
	}
}
