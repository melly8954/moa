package com.moa.api.repository.writer.role;

import java.util.Collection;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.moa.api.entity.role.AccessRole;
import com.moa.api.entity.role.AccessRoles;
import com.moa.api.entity.role.UserAccessRole;
import com.moa.api.repository.jpa.role.AccessRoleRepository;
import com.moa.api.repository.jpa.role.UserAccessRoleRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccessRoleWriter {

	private final AccessRoleRepository accessRoleRepository;
	private final UserAccessRoleRepository userAccessRoleRepository;

	public AccessRole save(AccessRole role) {
		return accessRoleRepository.save(role);
	}

	/**
	 * moa: 최고 관리자 역할이 없으면 만든다 (specs/003-admin-roles FR-010). 운영에서는 V7 마이그레이션이 넣어 두므로
	 * 첫 관리자 부트스트랩과 인수 테스트(표를 비운 뒤)에서만 만든다
	 */
	public AccessRole ensureSuperAdminRole() {
		return accessRoleRepository.findByCode(AccessRoles.SUPER_ADMIN_CODE)
			.orElseGet(() -> accessRoleRepository.save(new AccessRole(AccessRoles.SUPER_ADMIN_CODE,
				AccessRoles.SUPER_ADMIN_NAME, AccessRoles.SUPER_ADMIN_DESCRIPTION, Set.of())));
	}

	/** 이 사용자의 역할을 통째로 바꾼다 */
	public void replaceRolesOfUser(Long userId, Collection<Long> roleIds) {
		userAccessRoleRepository.deleteAllByUserId(userId);
		userAccessRoleRepository.saveAll(roleIds.stream().map(roleId -> new UserAccessRole(userId, roleId)).toList());
	}

	/** 이 사용자에게 역할 하나를 더한다 (moa, specs/003-admin-roles FR-006). 이미 있는지는 부르는 쪽이 본다 */
	public void addRoleToUser(Long userId, Long roleId) {
		userAccessRoleRepository.save(new UserAccessRole(userId, roleId));
	}

	/** 이 사용자에게서 역할 하나를 뺀다 (moa, FR-006) */
	public void removeRoleFromUser(Long userId, Long roleId) {
		userAccessRoleRepository.deleteByUserIdAndAccessRoleId(userId, roleId);
	}

	/** 지운 역할을 사용자들에게서 뺀다 */
	public void removeRoleFromUsers(Long roleId) {
		userAccessRoleRepository.deleteAllByAccessRoleId(roleId);
	}
}
