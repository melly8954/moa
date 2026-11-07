package com.moa.api.dto.code;

import com.moa.api.entity.code.CodeGroup;

public record CodeGroupDto(String groupCode, String name, String description) {

	public static CodeGroupDto from(CodeGroup group) {
		return new CodeGroupDto(group.getGroupCode(), group.getName(), group.getDescription());
	}
}
