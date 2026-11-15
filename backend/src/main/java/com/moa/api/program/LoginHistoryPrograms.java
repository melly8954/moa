package com.moa.api.program;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.moa.api.permission.LoginHistoryPermissions;

/** login-history 헬퍼의 프로그램 */
@Component
@Order(40)
public class LoginHistoryPrograms implements ProgramSource {

	@Override
	public List<ProgramDefinition> programs() {
		return List.of(ProgramDefinition.admin("ADMIN_LOGIN_HISTORIES", "접속 이력", "/admin/login-histories",
			LoginHistoryPermissions.READ));
	}
}
