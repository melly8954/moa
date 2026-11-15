package com.moa.api.program;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.moa.api.permission.RolePermissions;

/** rbac 헬퍼의 프로그램 */
@Component
@Order(50)
public class RolePrograms implements ProgramSource {

	@Override
	public List<ProgramDefinition> programs() {
		return List.of(ProgramDefinition.admin("ADMIN_ROLES", "역할 관리", "/admin/roles", RolePermissions.MANAGE));
	}
}
