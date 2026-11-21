package com.moa.api.service.role;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moa.api.dto.role.AccessRoleDto;
import com.moa.api.dto.role.GrantBlock;
import com.moa.api.entity.role.AccessRole;
import com.moa.api.entity.role.AccessRoles;
import com.moa.api.repository.reader.role.AccessRoleReader;
import com.moa.api.repository.writer.role.AccessRoleWriter;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.permission.PermissionRegistry;

import lombok.RequiredArgsConstructor;

/**
 * 업무 역할 관리 (선택 헬퍼 rbac). 바꾸는 요청은 RoleGrantPolicy로 권한 상승을 막는다 (거부하면 A005).
 * moa: 최고 관리자 역할은 바꾸거나 지울 수 없고, 구성원은 첫 관리자 한 명으로 고정이며 화면·API로 더하거나 빼지 못한다.
 * 그 구성원은 다른 역할을 받지 않는다 (specs/003-admin-roles FR-010, research R16·R17·R23). DB도 유일 제약으로 한 명을 지킨다 (V8)
 */
@Service
@RequiredArgsConstructor
public class AccessRoleService {

	private final AccessRoleReader accessRoleReader;
	private final AccessRoleWriter accessRoleWriter;
	private final PermissionRegistry permissionRegistry;
	private final RoleGrantPolicy roleGrantPolicy;

	/** 만든 순서. moa: 최고 관리자 역할을 맨 앞에 둔다 (FR-005) */
	public List<AccessRoleDto> getAll() {
		Map<Long, Long> counts = accessRoleReader.countUsersByRole();
		return accessRoleReader.findAll().stream()
			.map(role -> dto(role, counts))
			.sorted(Comparator.comparing(AccessRoleDto::fixed).reversed())
			.toList();
	}

	/** moa: 다룰 수 없으면 그 이유, 다룰 수 있으면 null (research R18) */
	public GrantBlock blockReason(AuthPrincipal actor, AccessRoleDto role) {
		return roleGrantPolicy.blockReason(actor, role);
	}

	/**
	 * @throws ServiceException 없으면 R001
	 */
	public AccessRoleDto get(Long id) {
		return dto(require(id), accessRoleReader.countUsersByRole());
	}

	/**
	 * @throws ServiceException 등록되지 않은 권한 V001, 내가 줄 수 없는 권한 A005, 코드가 겹치면 R002
	 */
	public AccessRoleDto create(AuthPrincipal actor, String code, String name, String description,
		Collection<String> permissions) {
		Set<String> checked = checkPermissions(permissions);
		checkGrant(actor, checked);
		if (accessRoleReader.existsCode(code)) {
			throw new ServiceException(ErrorCode.DUPLICATE_RESOURCE, "이미 쓰는 역할 코드입니다: " + code);
		}
		return AccessRoleDto.of(accessRoleWriter.save(new AccessRole(code, name, description, checked)), 0);
	}

	/**
	 * 코드는 바꾸지 않는다. 바꾸기 전의 역할과 바꾼 뒤의 권한을 모두 다룰 수 있어야 한다.
	 *
	 * @throws ServiceException 없으면 R001, 등록되지 않은 권한 V001, 다룰 수 없는 역할·권한 A005
	 */
	public AccessRoleDto modify(AuthPrincipal actor, Long id, String name, String description,
		Collection<String> permissions) {
		AccessRole role = require(id);
		requireNotFixed(role);
		Map<Long, Long> counts = accessRoleReader.countUsersByRole();
		checkManage(actor, dto(role, counts));
		Set<String> checked = checkPermissions(permissions);
		checkGrant(actor, checked);
		role.modify(name, description, checked);
		return dto(role, counts);
	}

	/**
	 * 소프트 삭제하고, 이 역할을 가진 사용자에게서 뺀다.
	 *
	 * @throws ServiceException 없으면 R001, 다룰 수 없는 역할 A005
	 */
	public void delete(AuthPrincipal actor, Long id) {
		AccessRole role = require(id);
		requireNotFixed(role);
		checkManage(actor, dto(role, Map.of()));
		role.delete(actor.userId());
		accessRoleWriter.removeRoleFromUsers(id);
	}

	/**
	 * 여러 역할을 한꺼번에 지운다 (moa, specs/003-admin-roles FR-005). 하나라도 없거나 다룰 수 없으면 아무것도 지우지 않는다
	 * (호출하는 Application의 트랜잭션이 롤백한다). 지운 역할은 사용자들에게서도 빠진다.
	 *
	 * @throws ServiceException 없는 역할 R001, 다룰 수 없는 역할 A005
	 */
	public void deleteAll(AuthPrincipal actor, Collection<Long> ids) {
		List<AccessRole> roles = new ArrayList<>();
		for (Long id : new LinkedHashSet<>(ids)) {
			AccessRole role = require(id);
			requireNotFixed(role);
			checkManage(actor, dto(role, Map.of()));
			roles.add(role);
		}
		// 모두 지운 뒤에 사용자에게서 뺀다. removeRoleFromUsers는 영속성 컨텍스트를 비우므로(clearAutomatically)
		// 번갈아 하면 뒤의 역할이 분리돼 지운 표시가 저장되지 않는다
		roles.forEach(role -> role.delete(actor.userId()));
		roles.forEach(role -> accessRoleWriter.removeRoleFromUsers(role.getId()));
	}

	/** 이 사용자에게 준 역할 (지운 역할은 빠진다) */
	public List<AccessRoleDto> getRolesOfUser(Long userId) {
		Map<Long, Long> counts = accessRoleReader.countUsersByRole();
		return accessRoleReader.findAll(accessRoleReader.findRoleIdsOfUser(userId)).stream()
			.map(role -> dto(role, counts))
			.toList();
	}

	/**
	 * 이 사용자의 역할을 통째로 바꾼다. 빈 목록이면 모두 뺀다. 사용자가 있는지는 부르는 쪽이 본다.
	 * 새로 주는 역할과 빼는 역할을 모두 다룰 수 있어야 한다 (그대로 두는 역할은 보지 않는다).
	 *
	 * @throws ServiceException 없는 역할이 있으면 R001, 다룰 수 없는 역할 A005
	 */
	public void replaceRolesOfUser(AuthPrincipal actor, Long userId, Collection<Long> roleIds) {
		Set<Long> requested = new LinkedHashSet<>(roleIds);
		List<AccessRole> found = accessRoleReader.findAll(requested);
		if (found.size() != requested.size()) {
			throw new ServiceException(ErrorCode.NOT_FOUND, "없는 역할이 있습니다.");
		}
		Set<Long> current = new HashSet<>(accessRoleReader.findRoleIdsOfUser(userId));
		Set<Long> changed = new HashSet<>(requested);
		changed.addAll(current);
		changed.removeIf(id -> requested.contains(id) && current.contains(id));
		List<AccessRole> changedRoles = accessRoleReader.findAll(changed);
		// moa: 최고 관리자 역할을 주거나 빼는 요청은 누구든 막는다. 최고 관리자 계정은 다른 역할도 받지 않는다 (FR-010)
		if (changedRoles.stream().anyMatch(role -> AccessRoles.isSuperAdmin(role.getCode()))
			|| (!changed.isEmpty() && accessRoleReader.isSuperAdmin(userId))) {
			throw new ServiceException(ErrorCode.INVALID_STATE, "최고 관리자는 한 명으로 고정이에요. 바꾸려면 개발자에게 요청해요.");
		}
		changedRoles.forEach(role -> checkManage(actor, dto(role, Map.of())));
		accessRoleWriter.replaceRolesOfUser(userId, requested);
	}

	/**
	 * 이 역할에 여러 사용자를 더한다 (moa, specs/003-admin-roles FR-006). 이미 구성원이면 그대로 둔다.
	 * 사용자가 있는지와 ADMIN이 아닌지는 부르는 쪽이 본다.
	 *
	 * @throws ServiceException 없는 역할 R001, 다룰 수 없는 역할 A005
	 */
	public void addMembers(AuthPrincipal actor, Long roleId, Collection<Long> userIds) {
		AccessRole role = require(roleId);
		requireMembersChangeable(role);
		checkManage(actor, dto(role, Map.of()));
		if (!accessRoleReader.findSuperAdminIds(userIds).isEmpty()) {
			throw new ServiceException(ErrorCode.INVALID_STATE, "최고 관리자는 다른 역할을 받지 않아요. 이미 모든 권한을 가졌어요.");
		}
		for (Long userId : new LinkedHashSet<>(userIds)) {
			if (!accessRoleReader.hasRole(userId, roleId)) {
				accessRoleWriter.addRoleToUser(userId, roleId);
			}
		}
	}

	/**
	 * 이 역할에서 여러 사용자를 뺀다 (moa, FR-006). 구성원이 아니면 그대로 둔다.
	 *
	 * @throws ServiceException 없는 역할 R001, 다룰 수 없는 역할 A005
	 */
	public void removeMembers(AuthPrincipal actor, Long roleId, Collection<Long> userIds) {
		AccessRole role = require(roleId);
		requireMembersChangeable(role);
		checkManage(actor, dto(role, Map.of()));
		new LinkedHashSet<>(userIds).forEach(userId -> accessRoleWriter.removeRoleFromUser(userId, roleId));
	}

	/**
	 * @throws ServiceException 없는 역할 R001
	 */
	public void requireExists(Long roleId) {
		require(roleId);
	}

	/** moa: 최고 관리자 역할에 구성원이 있는가 (첫 관리자 부트스트랩, `002-admin-users` FR-001) */
	public boolean hasSuperAdmin() {
		return accessRoleReader.findSuperAdminRole().map(role -> accessRoleReader.countMembers(role.getId()) > 0)
			.orElse(false);
	}

	/** moa: 이 계정이 최고 관리자 역할을 가졌나 */
	public boolean isSuperAdmin(Long userId) {
		return accessRoleReader.isSuperAdmin(userId);
	}

	/** moa: 이 계정들 중 최고 관리자 */
	public Set<Long> findSuperAdminIds(Collection<Long> userIds) {
		return accessRoleReader.findSuperAdminIds(userIds);
	}

	/** moa: 계정 ID → 받은 역할들의 권한 합집합 (사용자 관리의 다루기 범위, `002-admin-users` research R10). 쿼리 2개 */
	public Map<Long, Set<String>> permissionsOf(Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, List<Long>> roleIds = accessRoleReader.findRoleIdsOfUsers(userIds);
		Map<Long, Set<String>> rolePermissions = new HashMap<>();
		accessRoleReader.findAll(roleIds.values().stream().flatMap(List::stream).collect(Collectors.toSet()))
			.forEach(role -> rolePermissions.put(role.getId(), role.getPermissions()));
		Map<Long, Set<String>> result = new HashMap<>();
		roleIds.forEach((userId, ids) -> result.put(userId, ids.stream()
			.flatMap(id -> rolePermissions.getOrDefault(id, Set.of()).stream())
			.collect(Collectors.toSet())));
		return result;
	}

	/** moa: 이 계정을 최고 관리자 역할에 넣는다 (첫 관리자 부트스트랩). 역할이 없으면 만들고, 다른 역할은 뺀다 */
	public void addSuperAdmin(Long userId) {
		accessRoleWriter.replaceRolesOfUser(userId, List.of(accessRoleWriter.ensureSuperAdminRole().getId()));
	}

	/** moa: 이 계정의 역할을 모두 뺀다 (관리자 계정을 회원으로 바꿀 때, `002-admin-users` FR-005) */
	public void removeAllRolesOf(Long userId) {
		accessRoleWriter.replaceRolesOfUser(userId, List.of());
	}

	/** moa: 최고 관리자 역할의 구성원은 화면·API로 바꾸지 못한다. 최고 관리자 자신도 (FR-010, 2026-11-21 한 명 고정) */
	private void requireMembersChangeable(AccessRole role) {
		if (AccessRoles.isSuperAdmin(role.getCode())) {
			throw new ServiceException(ErrorCode.INVALID_STATE, "최고 관리자는 한 명으로 고정이에요. 바꾸려면 개발자에게 요청해요.");
		}
	}

	private void requireNotFixed(AccessRole role) {
		if (AccessRoles.isSuperAdmin(role.getCode())) {
			throw new ServiceException(ErrorCode.INVALID_STATE, "최고 관리자 역할은 바꾸거나 지울 수 없어요.");
		}
	}

	private AccessRole require(Long id) {
		return accessRoleReader.find(id).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
	}

	private AccessRoleDto dto(AccessRole role, Map<Long, Long> counts) {
		return AccessRoleDto.of(role, counts.getOrDefault(role.getId(), 0L));
	}

	private Set<String> checkPermissions(Collection<String> permissions) {
		Set<String> checked = new LinkedHashSet<>();
		for (String permission : permissions) {
			if (!permissionRegistry.contains(permission)) {
				throw new ServiceException(ErrorCode.INVALID_INPUT, "등록되지 않은 권한입니다: " + permission);
			}
			checked.add(permission);
		}
		return checked;
	}

	private void checkGrant(AuthPrincipal actor, Collection<String> permissions) {
		if (!roleGrantPolicy.canGrant(actor, permissions)) {
			throw new ServiceException(ErrorCode.FORBIDDEN, "가지지 않은 권한은 역할에 넣을 수 없습니다.");
		}
	}

	private void checkManage(AuthPrincipal actor, AccessRoleDto role) {
		if (!roleGrantPolicy.canManage(actor, role)) {
			throw new ServiceException(ErrorCode.FORBIDDEN, "다룰 수 없는 역할입니다: " + role.code());
		}
	}
}
