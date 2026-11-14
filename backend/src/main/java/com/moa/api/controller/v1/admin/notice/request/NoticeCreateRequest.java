package com.moa.api.controller.v1.admin.notice.request;

import com.moa.common.helper.richtext.SafeHtml;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "공지 만들기")
public record NoticeCreateRequest(
	@Schema(description = "제목", example = "점검 안내") @NotBlank @Size(max = 200) String title,
	@Schema(description = "본문 HTML. 허용한 서식만 남기고 걸러 저장한다 (rich-text)") @SafeHtml @NotBlank @Size(
		max = 20000) String content,
	@Schema(description = "상단 고정") boolean pinned,
	@Schema(description = "true면 바로 게시, false면 임시 저장") boolean publish) {
}
