package com.moa.api.controller.v1.admin.faq.response;

import java.time.OffsetDateTime;

import com.moa.api.dto.faq.FaqDto;
import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "FAQ (관리)")
public record FaqAdminResponse(
	@Schema(description = "FAQ ID") Long id,
	@Schema(description = "분류 코드") String categoryCode,
	@Schema(description = "분류 이름") String categoryName,
	@Schema(description = "질문") String question,
	@Schema(description = "답 HTML (저장 때 걸렀다)") String answer,
	@Schema(description = "순서") int sortOrder,
	@Schema(description = "켜짐") boolean enabled,
	@Schema(description = "등록한 시각") OffsetDateTime createdAt,
	@Schema(description = "고친 시각") OffsetDateTime updatedAt) {

	public static FaqAdminResponse from(FaqDto dto) {
		return new FaqAdminResponse(dto.id(), dto.categoryCode(), dto.categoryName(), dto.question(), dto.answer(),
			dto.sortOrder(), dto.enabled(), ApiTimes.of(dto.createdAt()), ApiTimes.of(dto.updatedAt()));
	}
}
