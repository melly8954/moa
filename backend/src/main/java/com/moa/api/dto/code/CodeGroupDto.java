package com.moa.api.dto.code;

import com.moa.api.codegroup.CodeGroupDefinition;

/** 등록된 코드 그룹 (moa, specs/004-admin-codes FR-004) */
public record CodeGroupDto(String groupCode, String name, String description, String usedBy) {

	public static CodeGroupDto from(CodeGroupDefinition group) {
		return new CodeGroupDto(group.groupCode(), group.name(), group.description(), group.usedBy());
	}
}
