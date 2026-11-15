package com.moa.api.program;

import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.moa.api.permission.CodePermissions;

/** code 헬퍼의 프로그램 */
@Component
@Order(30)
public class CodePrograms implements ProgramSource {

	@Override
	public List<ProgramDefinition> programs() {
		return List.of(ProgramDefinition.admin("ADMIN_CODES", "공통코드", "/admin/codes", CodePermissions.MANAGE));
	}
}
