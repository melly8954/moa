package com.moa.api.controller.v1.admin.code.request;

import com.moa.api.dto.code.CodeGroupOrderDto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "moa: 그룹 순서 바꾸기 한 줄 (specs/004-admin-codes FR-005)")
public record CodeGroupOrderRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "그룹 코드",
		example = "GAME_GENRE") @NotBlank String groupCode,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "그룹 순서",
		example = "0") @NotNull @Min(0) Integer sortOrder) {

	public CodeGroupOrderDto toDto() {
		return new CodeGroupOrderDto(groupCode, sortOrder);
	}
}
