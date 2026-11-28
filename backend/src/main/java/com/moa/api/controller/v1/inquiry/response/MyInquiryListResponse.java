package com.moa.api.controller.v1.inquiry.response;

import java.time.OffsetDateTime;

import com.moa.api.dto.inquiry.InquiryDto;
import com.moa.api.dto.inquiry.InquiryStatus;
import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 문의 목록 항목")
public record MyInquiryListResponse(
	@Schema(description = "문의 ID") Long id,
	@Schema(description = "분류 코드") String categoryCode,
	@Schema(description = "분류 이름 (꺼진 분류도 이름이 있다)") String categoryName,
	@Schema(description = "제목") String title,
	@Schema(description = "상태") InquiryStatus status,
	@Schema(description = "쓴 시각") OffsetDateTime createdAt) {

	public static MyInquiryListResponse from(InquiryDto dto) {
		return new MyInquiryListResponse(dto.id(), dto.categoryCode(), dto.categoryName(), dto.title(), dto.status(),
			ApiTimes.of(dto.createdAt()));
	}
}
