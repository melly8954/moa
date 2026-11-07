package com.moa.common.security.permission;

import java.util.List;

/**
 * 권한을 등록한다. 헬퍼·프로젝트가 자기 권한을 이 빈으로 내놓으면 PermissionRegistry가 모은다.
 * 키는 상수로 두고 @RequirePermission에도 그 상수를 쓴다 (예: CodePermissions.MANAGE).
 */
@FunctionalInterface
public interface PermissionSource {

	List<PermissionDefinition> permissions();
}
