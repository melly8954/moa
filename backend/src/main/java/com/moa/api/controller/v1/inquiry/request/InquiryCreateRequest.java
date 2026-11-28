package com.moa.api.controller.v1.inquiry.request;

import java.util.List;

import com.moa.api.dto.inquiry.InquiryContent;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "1:1 문의 쓰기 (specs/017-customer-center FR-007)")
public record InquiryCreateRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "분류 (SUPPORT_CATEGORY 항목 코드)",
		example = "ACCOUNT") @NotBlank @Size(max = 50) String categoryCode,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "제목",
		example = "로그인이 안 돼요") @NotBlank @Size(max = 100) String title,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "내용 (서식 없는 글, 줄바꿈 유지)",
		example = "어제부터 구글 로그인이 실패해요.") @NotBlank @Size(max = 2000) String content,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
		description = "첨부 이미지 주소 (본문 이미지 올리기로 받은 url), 다섯 장까지",
		example = "[\"/api/v1/rich-text/images/3f2b8c1e-9a4d-4e7b-8c2a-1d5e6f7a8b9c\"]") @NotNull @Size(
			max = 5) List<@NotBlank @Size(max = 100) String> imageUrls) {

	public InquiryContent toContent() {
		return new InquiryContent(categoryCode, title, content, imageUrls);
	}
}
