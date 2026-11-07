package com.moa.api.application.v1.code.command;

public record ModifyCodeCommand(String groupCode, String code, String name, int sortOrder, boolean enabled) {
}
