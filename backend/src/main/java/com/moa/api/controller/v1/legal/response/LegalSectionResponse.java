package com.moa.api.controller.v1.legal.response;

import com.moa.common.legal.LegalSection;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "문서의 장 하나")
public record LegalSectionResponse(
	@Schema(description = "장 바로 가기 주소(#id)", example = "retention") String id,
	@Schema(description = "장 제목. 목차에 쓴다", example = "개인정보의 보유 기간") String heading,
	@Schema(description = "장 안의 HTML (제목 h2를 뺀 것). 문단, h3, 목록, 표, 링크") String html) {

	public static LegalSectionResponse from(LegalSection section) {
		return new LegalSectionResponse(section.id(), section.heading(), section.html());
	}
}
