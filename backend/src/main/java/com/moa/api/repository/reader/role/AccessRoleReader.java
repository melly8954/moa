package com.moa.api.repository.reader.role;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.moa.api.entity.role.AccessRole;
import com.moa.api.entity.role.AccessRoles;
import com.moa.api.entity.role.UserAccessRole;
import com.moa.api.repository.jpa.role.AccessRoleRepository;
import com.moa.api.repository.jpa.role.UserAccessRoleRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccessRoleReader {

	private final AccessRoleRepository accessRoleRepository;
	private final UserAccessRoleRepository userAccessRoleRepository;

	/** 지우지 않은 역할 전부, 만든 순서대로 */
	public List<AccessRole> findAll() {
		return accessRoleRepository.findAllByOrderByIdAsc();
	}

	public Optional<AccessRole> find(Long id) {
		return accessRoleRepository.findById(id);
	}

	/** 지우지 않은 것만. 없는 ID는 빠진다 */
	public List<AccessRole> findAll(Collection<Long> ids) {
		return accessRoleRepository.findAllByIdIn(ids);
	}

	public boolean existsCode(String code) {
		return accessRoleRepository.existsByCode(code);
	}

	/** 이 사용자에게 준 역할 ID (지운 역할이 남아 있을 수 있다) */
	public List<Long> findRoleIdsOfUser(Long userId) {
		return userAccessRoleRepository.findAllByUserId(userId).stream().map(UserAccessRole::getAccessRoleId).toList();
	}

	/** 사용자 ID → 그 사용자에게 준 역할 ID (moa, specs/003-admin-roles FR-005) */
	public Map<Long, List<Long>> findRoleIdsOfUsers(Collection<Long> userIds) {
		Map<Long, List<Long>> roleIds = new HashMap<>();
		for (UserAccessRole given : userAccessRoleRepository.findAllByUserIdIn(userIds)) {
			roleIds.computeIfAbsent(given.getUserId(), key -> new ArrayList<>()).add(given.getAccessRoleId());
		}
		return roleIds;
	}

	/** moa: 최고 관리자 역할 (specs/003-admin-roles FR-010). V7 마이그레이션이 넣는다. 인수 테스트는 표를 비워 없을 수 있다 */
	public Optional<AccessRole> findSuperAdminRole() {
		return accessRoleRepository.findByCode(AccessRoles.SUPER_ADMIN_CODE);
	}

	/** moa: 이 사용자가 최고 관리자 역할을 가졌나 */
	public boolean isSuperAdmin(Long userId) {
		return findSuperAdminRole().map(role -> hasRole(userId, role.getId())).orElse(false);
	}

	/** moa: 이 사용자들 중 최고 관리자 역할을 가진 사용자 */
	public Set<Long> findSuperAdminIds(Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return Set.of();
		}
		return findSuperAdminRole()
			.map(role -> userAccessRoleRepository.findAllByAccessRoleIdAndUserIdIn(role.getId(), userIds).stream()
				.map(UserAccessRole::getUserId)
				.collect(Collectors.toSet()))
			.orElse(Set.of());
	}

	/** moa: 이 역할의 구성원 수 */
	public long countMembers(Long roleId) {
		return userAccessRoleRepository.countByAccessRoleId(roleId);
	}

	public boolean hasRole(Long userId, Long roleId) {
		return userAccessRoleRepository.existsByUserIdAndAccessRoleId(userId, roleId);
	}

	public List<String> findPermissionsOfUser(Long userId) {
		return accessRoleRepository.findPermissionsByUserId(userId);
	}

	/** 역할 ID → 그 역할을 가진 사용자 수. 사용자가 없는 역할은 빠진다 */
	public Map<Long, Long> countUsersByRole() {
		Map<Long, Long> counts = new HashMap<>();
		for (Object[] row : userAccessRoleRepository.countUsersByRole()) {
			counts.put((Long)row[0], (Long)row[1]);
		}
		return counts;
	}
}
