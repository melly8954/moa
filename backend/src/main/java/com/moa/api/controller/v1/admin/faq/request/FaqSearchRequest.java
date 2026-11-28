package com.moa.api.controller.v1.admin.faq.request;

import org.jspecify.annotations.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "FAQ 관리 검색 조건. 모두 선택")
public record FaqSearchRequest(
	@Schema(description = "분류 코드", example = "ACCOUNT") @Size(max = 50) @Nullable String categoryCode,
	@Schema(description = "켜짐. 비면 모두") @Nullable Boolean enabled,
	@Schema(description = "질문에 들어 있는 글자", example = "비밀번호") @Size(max = 200) @Nullable String keyword) {
}
