package com.moa.api.repository.reader.role;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.moa.api.entity.role.AccessRole;
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
