package com.moa.api.controller.v1.admin.inquiry.response;

import java.time.OffsetDateTime;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.inquiry.InquiryDto;
import com.moa.api.dto.inquiry.InquiryStatus;
import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "문의 (관리)")
public record InquiryAdminResponse(
	@Schema(description = "문의 ID") Long id,
	@Schema(description = "상태") InquiryStatus status,
	@Schema(description = "분류 코드") String categoryCode,
	@Schema(description = "분류 이름") String categoryName,
	@Schema(description = "제목") String title,
	@Schema(description = "내용 (서식 없는 글)") String content,
	@Schema(description = "첨부 이미지 주소") List<String> imageUrls,
	@Schema(description = "쓴 회원") @Nullable InquiryAuthorResponse author,
	@Schema(description = "쓴 시각") OffsetDateTime createdAt,
	@Schema(description = "답변 HTML. 답변 대기면 없다") @Nullable String answer,
	@Schema(description = "처음 답변한 시각") @Nullable OffsetDateTime answeredAt,
	@Schema(description = "처음 답변한 관리자 이름") @Nullable String answeredByName,
	@Schema(description = "답변할 수 있는지 (탈퇴한 회원의 문의는 아니다, US5)") boolean answerable) {

	public static InquiryAdminResponse from(InquiryDto dto) {
		return new InquiryAdminResponse(dto.id(), dto.status(), dto.categoryCode(), dto.categoryName(), dto.title(),
			dto.content(), dto.imageUrls(), InquiryAuthorResponse.from(dto.author()), ApiTimes.of(dto.createdAt()),
			dto.answer(), ApiTimes.of(dto.answeredAt()), dto.answeredByName(), dto.answerable());
	}
}
