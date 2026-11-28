package com.moa.api.controller.v1.inquiry.response;

import java.time.OffsetDateTime;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.inquiry.InquiryDto;
import com.moa.api.dto.inquiry.InquiryStatus;
import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 문의 (회원). 답변한 관리자는 보이지 않는다 (FR-008)")
public record MyInquiryResponse(
	@Schema(description = "문의 ID") Long id,
	@Schema(description = "분류 코드") String categoryCode,
	@Schema(description = "분류 이름") String categoryName,
	@Schema(description = "제목") String title,
	@Schema(description = "내용 (서식 없는 글)") String content,
	@Schema(description = "첨부 이미지 주소") List<String> imageUrls,
	@Schema(description = "상태") InquiryStatus status,
	@Schema(description = "쓴 시각") OffsetDateTime createdAt,
	@Schema(description = "답변 HTML (저장 때 걸렀다). 답변 대기면 없다") @Nullable String answer,
	@Schema(description = "답변 시각") @Nullable OffsetDateTime answeredAt,
	@Schema(description = "지울 수 있는지 (답변 대기)") boolean deletable) {

	public static MyInquiryResponse from(InquiryDto dto) {
		return new MyInquiryResponse(dto.id(), dto.categoryCode(), dto.categoryName(), dto.title(), dto.content(),
			dto.imageUrls(), dto.status(), ApiTimes.of(dto.createdAt()), dto.answer(), ApiTimes.of(dto.answeredAt()),
			dto.deletable());
	}
}
