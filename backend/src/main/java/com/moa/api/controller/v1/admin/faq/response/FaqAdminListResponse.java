package com.moa.api.controller.v1.admin.faq.response;

import java.time.OffsetDateTime;

import com.moa.api.dto.faq.FaqDto;
import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "FAQ 관리 목록 항목")
public record FaqAdminListResponse(
	@Schema(description = "FAQ ID") Long id,
	@Schema(description = "분류 코드") String categoryCode,
	@Schema(description = "분류 이름 (꺼진 분류도 이름이 있다)") String categoryName,
	@Schema(description = "질문") String question,
	@Schema(description = "순서") int sortOrder,
	@Schema(description = "켜짐") boolean enabled,
	@Schema(description = "고친 시각") OffsetDateTime updatedAt) {

	public static FaqAdminListResponse from(FaqDto dto) {
		return new FaqAdminListResponse(dto.id(), dto.categoryCode(), dto.categoryName(), dto.question(),
			dto.sortOrder(), dto.enabled(), ApiTimes.of(dto.updatedAt()));
	}
}
