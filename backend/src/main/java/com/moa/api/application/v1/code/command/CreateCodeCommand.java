package com.moa.api.application.v1.code.command;

import org.jspecify.annotations.Nullable;

/**
 * @param code 관리자가 적는다. 자동 순번 없음 (moa, specs/004-admin-codes FR-006 2026-11-21)
 * @param sortOrder 비우면 그룹 맨 뒤
 */
public record CreateCodeCommand(String groupCode, String code, String name, @Nullable Integer sortOrder) {
}
