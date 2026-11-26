package com.moa.api.controller.v1.admin.inquiry.response;

import java.time.OffsetDateTime;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.inquiry.InquiryDto;
import com.moa.api.dto.inquiry.InquiryStatus;
import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "문의 관리 목록 항목")
public record InquiryAdminListResponse(
	@Schema(description = "문의 ID") Long id,
	@Schema(description = "상태") InquiryStatus status,
	@Schema(description = "분류 코드") String categoryCode,
	@Schema(description = "분류 이름") String categoryName,
	@Schema(description = "제목") String title,
	@Schema(description = "쓴 회원") @Nullable InquiryAuthorResponse author,
	@Schema(description = "쓴 시각") OffsetDateTime createdAt,
	@Schema(description = "답변 시각") @Nullable OffsetDateTime answeredAt) {

	public static InquiryAdminListResponse from(InquiryDto dto) {
		return new InquiryAdminListResponse(dto.id(), dto.status(), dto.categoryCode(), dto.categoryName(),
			dto.title(), InquiryAuthorResponse.from(dto.author()), ApiTimes.of(dto.createdAt()),
			ApiTimes.of(dto.answeredAt()));
	}
}
