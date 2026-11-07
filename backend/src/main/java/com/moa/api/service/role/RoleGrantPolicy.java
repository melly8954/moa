package com.moa.api.service.role;

import java.util.Collection;

import com.moa.api.dto.role.AccessRoleDto;
import com.moa.common.security.AuthPrincipal;

/**
 * 역할을 다룰 수 있는지 정한다 (선택 헬퍼 rbac). 권한 상승을 막는다.
 * 기본 구현은 PermissionCeilingGrantPolicy("자기가 가진 권한까지만"). 역할 등급 같은 규칙이 필요하면
 * 이 인터페이스를 구현해 @Primary 빈으로 등록한다. 거부하면 AccessRoleService가 A005로 응답한다.
 */
public interface RoleGrantPolicy {

	/** 역할이 이 권한들을 갖게 만들 수 있는가 (역할 만들기, 바꾼 뒤의 권한) */
	boolean canGrant(AuthPrincipal actor, Collection<String> permissions);

	/** 이미 있는 이 역할을 다룰 수 있는가 (바꾸기·지우기, 사용자에게 주기·빼기) */
	boolean canManage(AuthPrincipal actor, AccessRoleDto role);
}
