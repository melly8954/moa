package com.moa.api.application.v1.code.command;

import org.jspecify.annotations.Nullable;

/**
 * @param code 비우면 자동 순번 (moa, specs/004-admin-codes FR-006)
 * @param sortOrder 비우면 그룹 맨 뒤
 */
public record CreateCodeCommand(String groupCode, @Nullable String code, String name, @Nullable Integer sortOrder) {
}
