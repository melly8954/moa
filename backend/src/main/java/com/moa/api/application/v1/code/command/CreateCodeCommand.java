package com.moa.api.application.v1.code.command;

public record CreateCodeCommand(String groupCode, String code, String name, int sortOrder) {
}
