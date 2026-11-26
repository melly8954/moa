package com.moa.api.controller.v1.faq.response;

import com.moa.api.dto.faq.FaqDto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 FAQ 항목. 펼칠 때 따로 부르지 않도록 답까지 담는다")
public record FaqPublicResponse(
	@Schema(description = "FAQ ID") Long id,
	@Schema(description = "분류 코드") String categoryCode,
	@Schema(description = "분류 이름") String categoryName,
	@Schema(description = "질문") String question,
	@Schema(description = "답 HTML (저장 때 걸렀다)") String answer) {

	public static FaqPublicResponse from(FaqDto dto) {
		return new FaqPublicResponse(dto.id(), dto.categoryCode(), dto.categoryName(), dto.question(), dto.answer());
	}
}
