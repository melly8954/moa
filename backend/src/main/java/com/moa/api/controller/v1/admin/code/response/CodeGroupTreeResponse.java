package com.moa.api.controller.v1.admin.code.response;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.moa.api.controller.v1.code.response.CodeListResponse;
import com.moa.api.dto.code.CodeGroupTreeDto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "moa: 공통코드 트리 표의 그룹 하나 (specs/004-admin-codes FR-005)")
public record CodeGroupTreeResponse(
	@Schema(description = "그룹 코드", example = "GAME_GENRE") String groupCode,
	@Schema(description = "이름", example = "게임 장르") String name,
	@Schema(description = "설명") @Nullable String description,
	@Schema(description = "쓰는 곳", example = "게임 등록 화면의 장르") @Nullable String usedBy,
	@Schema(description = "시스템 그룹. 기능이 코드에서 읽어 지울 수 없다") boolean system,
	@Schema(description = "항목 (사용 안 함 포함, 정렬 순서대로)") List<CodeListResponse> codes) {

	public static CodeGroupTreeResponse from(CodeGroupTreeDto dto) {
		return new CodeGroupTreeResponse(dto.group().groupCode(), dto.group().name(), dto.group().description(),
			dto.group().usedBy(), dto.group().system(), dto.codes().stream().map(CodeListResponse::from).toList());
	}
}
