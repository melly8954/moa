package com.moa.api.controller.v1.admin.inquiry.request;

import com.moa.common.helper.richtext.SafeHtml;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "문의 답변 쓰기·고치기 (specs/017-customer-center FR-008)")
public record InquiryAnswerRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
		description = "답변 HTML. 허용한 서식만 남기고 걸러 저장한다 (rich-text)") @SafeHtml @NotBlank @Size(
			max = 20000) String content) {
}
