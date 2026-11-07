package com.moa.common.security.permission;

import java.util.Set;

/**
 * 사용자에게 준 권한을 읽는다. rbac 헬퍼가 구현해 빈으로 등록한다.
 * 구현이 없으면 ADMIN만 모든 권한을 갖고, 다른 사용자는 권한이 없다.
 */
public interface PermissionChecker {

	/** 이 사용자에게 준 권한 키. ADMIN 판정은 PermissionGuard가 먼저 하므로 여기서는 신경 쓰지 않는다 */
	Set<String> permissions(Long userId);
}
