package com.moa.api.controller.v1.faq.response;

import com.moa.api.dto.support.SupportCategories.SupportCategory;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "고객센터 분류 (지금 고를 수 있는 항목)")
public record SupportCategoryResponse(
	@Schema(description = "항목 코드", example = "ACCOUNT") String code,
	@Schema(description = "이름", example = "계정") String name) {

	public static SupportCategoryResponse from(SupportCategory category) {
		return new SupportCategoryResponse(category.code(), category.name());
	}
}
