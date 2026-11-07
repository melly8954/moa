package com.moa.api.controller.v1.admin.code.response;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.code.CodeGroupDto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "코드 그룹")
public record CodeGroupListResponse(
	@Schema(description = "그룹 코드", example = "ORDER_STATUS") String groupCode,
	@Schema(description = "이름", example = "주문 상태") String name,
	@Schema(description = "설명") @Nullable String description) {

	public static CodeGroupListResponse from(CodeGroupDto dto) {
		return new CodeGroupListResponse(dto.groupCode(), dto.name(), dto.description());
	}
}
