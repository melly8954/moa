package com.moa.api.application.v1.role.command;

import java.util.List;

public record ModifyAccessRoleCommand(Long id, String name, String description, List<String> permissions) {
}
