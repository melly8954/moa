package com.moa.api.controller.v1.admin.code.response;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.code.CodeGroupDto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "코드 그룹. moa: 코드에 등록한 그룹이다 (specs/004-admin-codes FR-004)")
public record CodeGroupListResponse(
	@Schema(description = "그룹 코드", example = "GAME_GENRE") String groupCode,
	@Schema(description = "이름", example = "게임 장르") String name,
	@Schema(description = "설명") @Nullable String description,
	@Schema(description = "moa: 쓰는 곳", example = "게임 등록 화면의 장르") String usedBy) {

	public static CodeGroupListResponse from(CodeGroupDto dto) {
		return new CodeGroupListResponse(dto.groupCode(), dto.name(), dto.description(), dto.usedBy());
	}
}
