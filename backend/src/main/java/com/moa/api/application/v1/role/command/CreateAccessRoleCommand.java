package com.moa.api.application.v1.role.command;

import java.util.List;

public record CreateAccessRoleCommand(String code, String name, String description, List<String> permissions) {
}
