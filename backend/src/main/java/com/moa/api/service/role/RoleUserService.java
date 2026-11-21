package com.moa.api.service.role;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moa.api.dto.role.RoleMemberDto;
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
 * 권한 관리에서 보는 사용자 (moa, specs/003-admin-roles FR-006·R14). 사용자 관리 권한 없이 권한 관리 권한으로 쓴다.
 */
@Service
@RequiredArgsConstructor
public class RoleUserService {

	private final RoleUserReader roleUserReader;
	private final AccessRoleReader accessRoleReader;

	/**
	 * 역할 "구성원" (moa, FR-006). keyword가 없으면 그 역할의 구성원만, 있으면 아이디·이름으로 관리자 계정. 아이디 순
	 *
	 * @throws ServiceException 없는 역할 R001
	 */
	public Page<RoleMemberDto> getMembers(Long systemActorId, Long roleId, String keyword, Pageable pageable) {
		accessRoleReader.find(roleId).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND));
		Page<User> users = keyword == null || keyword.isBlank()
			? roleUserReader.findMembersPage(systemActorId, roleId, pageable)
			: roleUserReader.findSearchPage(systemActorId, keyword, pageable);
		List<Long> ids = users.getContent().stream().map(User::getId).toList();
		Map<Long, List<Long>> roleIds = users.isEmpty() ? Map.of() : accessRoleReader.findRoleIdsOfUsers(ids);
		Set<Long> superAdmins = accessRoleReader.findSuperAdminIds(ids);
		return users.map(found -> new RoleMemberDto(found.getId(), found.getLoginId(), found.getName(),
			superAdmins.contains(found.getId()), roleIds.getOrDefault(found.getId(), List.of()).contains(roleId)));
	}

	/**
	 * 역할을 주거나 뺄 사용자들이 모두 있고 관리자 계정인지 본다 (moa, FR-006, 2026-11-21).
	 *
	 * @throws ServiceException 없는 사용자 R001, 회원(USER)이 섞이면 B001
	 */
	public void requireTargets(Long systemActorId, Collection<Long> userIds) {
		Set<Long> ids = new HashSet<>(userIds);
		List<User> users = roleUserReader.findAll(systemActorId, ids);
		if (users.size() != ids.size()) {
			throw new ServiceException(ErrorCode.NOT_FOUND, "없는 사용자가 있습니다.");
		}
		users.forEach(found -> requireAdminAccount(found.getRole()));
	}

	/** 사용자 ID → 받은 업무 역할 (만든 순서). 사용자 관리 목록의 업무 역할 열이 쓴다 (`002-admin-users` FR-006) */
	public Map<Long, List<RoleUserDto.RoleRef>> accessRolesOf(Collection<Long> userIds) {
		return rolesOf(userIds);
	}

	/**
	 * 회원(USER)은 역할을 받지 않는다 (moa, specs/003-admin-roles FR-002, 2026-11-21. 그 전에는 "ADMIN이면 B001"이었다).
	 *
	 * @throws ServiceException 대상이 USER면 B001
	 */
	public void requireAdminAccount(Role role) {
		if (role != Role.ADMIN) {
			throw new ServiceException(ErrorCode.INVALID_STATE, "회원에게는 역할을 줄 수 없어요. 계정 종류를 관리자로 바꿔 주세요.");
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

}
