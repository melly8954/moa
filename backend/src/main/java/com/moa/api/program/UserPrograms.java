package com.moa.api.program;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.moa.api.permission.UserPermissions;

/** user-admin 헬퍼의 프로그램 */
@Component
@Order(10)
public class UserPrograms implements ProgramSource {

	@Override
	public List<ProgramDefinition> programs() {
		return List.of(ProgramDefinition.admin("ADMIN_USERS", "사용자 관리", "/admin/users", UserPermissions.MANAGE));
	}
}
