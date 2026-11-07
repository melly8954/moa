package com.moa.api.service.role;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.moa.api.repository.reader.role.AccessRoleReader;
import com.moa.common.security.permission.PermissionChecker;

import lombok.RequiredArgsConstructor;

/**
 * 권한 판정 (선택 헬퍼 rbac). 사용자에게 준 역할들의 권한 합집합이다.
 * 요청마다 조회한다 (쿼리 1개). 캐시하지 않아 역할·권한을 바꾸면 바로 반영되고, 서버가 여러 대여도 어긋나지 않는다.
 */
@Component
@RequiredArgsConstructor
public class AccessRolePermissionChecker implements PermissionChecker {

	private final AccessRoleReader accessRoleReader;

	@Override
	public Set<String> permissions(Long userId) {
		return new HashSet<>(accessRoleReader.findPermissionsOfUser(userId));
	}
}
