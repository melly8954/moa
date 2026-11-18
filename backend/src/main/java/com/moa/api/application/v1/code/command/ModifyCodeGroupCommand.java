package com.moa.api.application.v1.code.command;

import org.jspecify.annotations.Nullable;

/** moa: 그룹 이름·설명·쓰는 곳. 그룹 코드와 시스템 여부는 바꾸지 않는다 (specs/004-admin-codes FR-004·FR-008) */
public record ModifyCodeGroupCommand(String groupCode, String name, @Nullable String description,
	@Nullable String usedBy) {
}
