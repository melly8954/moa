package com.moa.api.permission;

import java.util.List;

import org.springframework.stereotype.Component;

import com.moa.common.security.permission.PermissionDefinition;
import com.moa.common.security.permission.PermissionSource;

/** login-history 헬퍼의 권한 */
@Component
public class LoginHistoryPermissions implements PermissionSource {

	public static final String READ = "LOGIN_HISTORY_READ";

	@Override
	public List<PermissionDefinition> permissions() {
		return List.of(new PermissionDefinition(READ, "접속 이력 조회", "접속 이력"));
	}
}
