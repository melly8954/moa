package com.moa.api.repository.writer.role;

import java.util.Collection;

import org.springframework.stereotype.Component;

import com.moa.api.entity.role.AccessRole;
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

	/** 이 사용자의 역할을 통째로 바꾼다 */
	public void replaceRolesOfUser(Long userId, Collection<Long> roleIds) {
		userAccessRoleRepository.deleteAllByUserId(userId);
		userAccessRoleRepository.saveAll(roleIds.stream().map(roleId -> new UserAccessRole(userId, roleId)).toList());
	}

	/** 지운 역할을 사용자들에게서 뺀다 */
	public void removeRoleFromUsers(Long roleId) {
		userAccessRoleRepository.deleteAllByAccessRoleId(roleId);
	}
}
