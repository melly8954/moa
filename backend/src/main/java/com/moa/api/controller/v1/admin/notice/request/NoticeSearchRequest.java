package com.moa.api.controller.v1.admin.notice.request;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.notice.NoticeStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "공지 관리 검색 조건. 모두 선택")
public record NoticeSearchRequest(
	@Schema(description = "제목에 들어 있는 글자", example = "점검") @Size(max = 200) @Nullable String keyword,
	@Schema(description = "상태") @Nullable NoticeStatus status) {
}
