package com.moa.api.controller.v1.notice.response;

import java.time.OffsetDateTime;

import com.moa.api.dto.notice.NoticeDto;
import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공지")
public record NoticeResponse(
	@Schema(description = "공지 ID") Long id,
	@Schema(description = "제목") String title,
	@Schema(description = "본문 HTML (저장 때 걸렀다)") String content,
	@Schema(description = "상단 고정") boolean pinned,
	@Schema(description = "게시 시각") OffsetDateTime publishedAt) {

	public static NoticeResponse from(NoticeDto dto) {
		return new NoticeResponse(dto.id(), dto.title(), dto.content(), dto.pinned(),
			ApiTimes.of(dto.publishedAt()));
	}
}
