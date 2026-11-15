package com.moa.api.service.role;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moa.api.dto.role.RoleUserDto;
import com.moa.api.entity.role.AccessRole;
import com.moa.api.entity.user.User;
import com.moa.api.repository.reader.role.AccessRoleReader;
import com.moa.api.repository.reader.role.RoleUserReader;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;
import com.moa.common.security.Role;

import lombok.RequiredArgsConstructor;

/**
 * 역할 관리에서 보는 사용자 (moa, specs/003-admin-roles FR-005·FR-006). 사용자 관리 권한 없이 역할 관리 권한으로 쓴다.
 */
@Service
@RequiredArgsConstructor
public class RoleUserService {

	private final RoleUserReader roleUserReader;
	private final AccessRoleReader accessRoleReader;

	/** keyword가 없으면 업무 역할이 있는 사용자와 ADMIN, 있으면 아이디·이름으로 모든 사용자. 아이디 순 */
	public Page<RoleUserDto> getPage(Long systemActorId, String keyword, Pageable pageable) {
		Page<User> users = roleUserReader.findPage(systemActorId, keyword, pageable);
		Map<Long, List<RoleUserDto.RoleRef>> roles = rolesOf(users.getContent().stream().map(User::getId).toList());
		return users.map(user -> dto(user, roles));
	}

	/**
	 * 이 역할의 구성원, 아이디 순.
	 *
	 * @throws ServiceException 없는 역할 R001
	 */
	public List<RoleUserDto> getMembers(Long roleId) {
		accessRoleReader.find(roleId).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
		List<User> members = roleUserReader.findMembers(roleId);
		Map<Long, List<RoleUserDto.RoleRef>> roles = rolesOf(members.stream().map(User::getId).toList());
		return members.stream().map(user -> dto(user, roles)).toList();
	}

	/**
	 * ADMIN은 모든 권한을 가져 업무 역할을 주거나 빼지 않는다 (specs/003-admin-roles Edge Cases).
	 *
	 * @throws ServiceException 대상이 ADMIN이면 B001
	 */
	public void requireNotAdmin(Role role) {
		if (role == Role.ADMIN) {
			throw new ServiceException(ErrorCode.INVALID_STATE, "관리자에게는 업무 역할을 주거나 빼지 않습니다.");
		}
	}

	private Map<Long, List<RoleUserDto.RoleRef>> rolesOf(Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, List<Long>> roleIds = accessRoleReader.findRoleIdsOfUsers(userIds);
		Map<Long, AccessRole> roles = accessRoleReader
			.findAll(roleIds.values().stream().flatMap(List::stream).collect(Collectors.toSet())).stream()
			.collect(Collectors.toMap(AccessRole::getId, Function.identity()));
		return roleIds.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue()
			.stream()
			.sorted()
			.map(roles::get)
			.filter(Objects::nonNull)
			.map(role -> new RoleUserDto.RoleRef(role.getId(), role.getCode(), role.getName()))
			.toList()));
	}

	private RoleUserDto dto(User user, Map<Long, List<RoleUserDto.RoleRef>> roles) {
		return new RoleUserDto(user.getId(), user.getLoginId(), user.getName(), user.getRole(),
			roles.getOrDefault(user.getId(), List.of()));
	}
}
