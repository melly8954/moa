package com.moa.api.application.v1.code.command;

import org.jspecify.annotations.Nullable;

/** moa: 그룹 이름·설명·사용 여부. 그룹 코드는 바꾸지 않는다 (specs/004-admin-codes FR-004·FR-008) */
public record ModifyCodeGroupCommand(String groupCode, String name, @Nullable String description, boolean enabled) {
}
