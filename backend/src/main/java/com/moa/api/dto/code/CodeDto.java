package com.moa.api.dto.code;

import com.moa.api.entity.code.Code;

public record CodeDto(String groupCode, String code, String name, int sortOrder, boolean enabled) {

	public static CodeDto from(Code code) {
		return new CodeDto(code.getGroupCode(), code.getCode(), code.getName(), code.getSortOrder(),
			code.isEnabled());
	}
}
