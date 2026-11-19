package com.moa.api.dto.code;

import org.jspecify.annotations.Nullable;

import com.moa.api.entity.code.CodeGroup;

/**
 * 코드 그룹 (moa, specs/004-admin-codes FR-004·FR-008)
 *
 * @param system 기능이 코드에서 읽는 그룹. 지울 수 없다
 */
public record CodeGroupDto(String groupCode, String name, @Nullable String description, @Nullable String usedBy,
	boolean system) {

	public static CodeGroupDto from(CodeGroup group) {
		return new CodeGroupDto(group.getGroupCode(), group.getName(), group.getDescription(), group.getUsedBy(),
			group.isSystem());
	}
}
