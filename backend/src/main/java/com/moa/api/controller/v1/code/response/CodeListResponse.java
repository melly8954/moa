package com.moa.api.controller.v1.code.response;

import com.moa.api.dto.code.CodeDto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "코드")
public record CodeListResponse(
	@Schema(description = "코드", example = "PAID") String code,
	@Schema(description = "이름", example = "결제 완료") String name,
	@Schema(description = "정렬 순서", example = "10") int sortOrder,
	@Schema(description = "사용 여부") boolean enabled) {

	public static CodeListResponse from(CodeDto dto) {
		return new CodeListResponse(dto.code(), dto.name(), dto.sortOrder(), dto.enabled());
	}
}
