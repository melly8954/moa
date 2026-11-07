package com.moa.api.permission;

import java.util.List;

import org.springframework.stereotype.Component;

import com.moa.common.security.permission.PermissionDefinition;
import com.moa.common.security.permission.PermissionSource;

/** code 헬퍼의 권한 */
@Component
public class CodePermissions implements PermissionSource {

	public static final String MANAGE = "CODE_MANAGE";

	@Override
	public List<PermissionDefinition> permissions() {
		return List.of(new PermissionDefinition(MANAGE, "공통코드 관리", "공통코드"));
	}
}
