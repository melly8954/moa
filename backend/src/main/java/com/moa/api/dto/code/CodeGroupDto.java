package com.moa.api.dto.code;

import org.jspecify.annotations.Nullable;

import com.moa.api.entity.code.CodeGroup;

/**
 * 코드 그룹 (moa, specs/004-admin-codes FR-004·FR-008)
 *
 * @param enabled 사용 여부. 꺼지면 화면용 목록이 빈 목록이다
 */
public record CodeGroupDto(String groupCode, String name, @Nullable String description, boolean enabled) {

	public static CodeGroupDto from(CodeGroup group) {
		return new CodeGroupDto(group.getGroupCode(), group.getName(), group.getDescription(), group.isEnabled());
	}
}
