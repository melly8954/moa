package com.moa.api.controller.v1.admin.code.request;

import com.moa.api.dto.code.CodeOrderDto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "moa: 그룹 안 순서 바꾸기 한 줄")
public record CodeOrderRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "코드", example = "001") @NotBlank String code,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "정렬 순서",
		example = "0") @NotNull @Min(0) Integer sortOrder) {

	public CodeOrderDto toDto() {
		return new CodeOrderDto(code, sortOrder);
	}
}
