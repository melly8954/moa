package com.moa.api.application.v1.code.command;

import org.jspecify.annotations.Nullable;

/**
 * @param sortOrder 비우면 그대로 둔다 (moa)
 */
public record ModifyCodeCommand(String groupCode, String code, String name, @Nullable Integer sortOrder,
	boolean enabled) {
}
