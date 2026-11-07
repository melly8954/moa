package com.moa.api.service.role;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.moa.api.dto.role.AccessRoleDto;
import com.moa.api.entity.role.AccessRole;
import com.moa.api.repository.reader.role.AccessRoleReader;
import com.moa.api.repository.writer.role.AccessRoleWriter;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.permission.PermissionRegistry;

import lombok.RequiredArgsConstructor;

/**
 * 업무 역할 관리 (선택 헬퍼 rbac). 바꾸는 요청은 RoleGrantPolicy로 권한 상승을 막는다 (거부하면 A005).
 */
@Service
@RequiredArgsConstructor
public class AccessRoleService {

	private final AccessRoleReader accessRoleReader;
	private final AccessRoleWriter accessRoleWriter;
	private final PermissionRegistry permissionRegistry;
	private final RoleGrantPolicy roleGrantPolicy;

	public List<AccessRoleDto> getAll() {
		Map<Long, Long> counts = accessRoleReader.countUsersByRole();
		return accessRoleReader.findAll().stream().map(role -> dto(role, counts)).toList();
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
		checkManage(actor, dto(role, Map.of()));
		role.delete(actor.userId());
		accessRoleWriter.removeRoleFromUsers(id);
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
		if (accessRoleReader.findAll(requested).size() != requested.size()) {
			throw new ServiceException(ErrorCode.NOT_FOUND, "없는 역할이 있습니다.");
		}
		Set<Long> current = new HashSet<>(accessRoleReader.findRoleIdsOfUser(userId));
		Set<Long> changed = new HashSet<>(requested);
		changed.addAll(current);
		changed.removeIf(id -> requested.contains(id) && current.contains(id));
		accessRoleReader.findAll(changed).forEach(role -> checkManage(actor, dto(role, Map.of())));
		accessRoleWriter.replaceRolesOfUser(userId, requested);
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
