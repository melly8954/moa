package com.moa.api.controller.v1.admin.role.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "moa: 구성원으로 더하거나 뺄 사용자 ID 목록")
public record RoleMembersRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "사용자 ID들",
		example = "[10, 11]") @NotEmpty @Size(max = 100) List<@NotNull Long> userIds) {
}
