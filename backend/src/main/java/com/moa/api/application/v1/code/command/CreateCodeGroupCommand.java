package com.moa.api.application.v1.code.command;

import org.jspecify.annotations.Nullable;

/** moa: 관리자가 만드는 일반 그룹 (specs/004-admin-codes FR-004) */
public record CreateCodeGroupCommand(String groupCode, String name, @Nullable String description,
	@Nullable String usedBy) {
}
