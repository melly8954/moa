package com.moa.api.controller.v1.admin.faq.request;

import com.moa.api.dto.faq.FaqContent;
import com.moa.common.helper.richtext.SafeHtml;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "FAQ 등록·고치기 (specs/017-customer-center FR-004)")
public record FaqSaveRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "분류 (SUPPORT_CATEGORY 항목 코드)",
		example = "ACCOUNT") @NotBlank @Size(max = 50) String categoryCode,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "질문",
		example = "비밀번호를 잊었어요") @NotBlank @Size(max = 200) String question,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
		description = "답 HTML. 허용한 서식만 남기고 걸러 저장한다 (rich-text)") @SafeHtml @NotBlank @Size(
			max = 20000) String answer,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "순서 (작을수록 먼저)",
		example = "0") @NotNull @Min(0) @Max(9999) Integer sortOrder,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "켜짐 (사용자에게 보임)") @NotNull Boolean enabled) {

	public FaqContent toContent() {
		return new FaqContent(categoryCode, question, answer, sortOrder, enabled);
	}
}
