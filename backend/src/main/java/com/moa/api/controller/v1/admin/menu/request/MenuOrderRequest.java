package com.moa.api.controller.v1.admin.menu.request;

import com.moa.api.dto.menu.MenuOrderDto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "순서 바꾸기 한 줄")
public record MenuOrderRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "메뉴 ID") @NotNull Long id,
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "부모 메뉴 ID. 없으면 1단계") Long parentId,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "정렬 순서",
		example = "10") @NotNull @Min(0) Integer sortOrder) {

	public MenuOrderDto toDto() {
		return new MenuOrderDto(id, parentId, sortOrder);
	}
}
