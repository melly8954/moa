package com.moa.api.controller.v1.notice.response;

import java.time.OffsetDateTime;

import com.moa.api.dto.notice.NoticeDto;
import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공지 목록 항목")
public record NoticeListResponse(
	@Schema(description = "공지 ID") Long id,
	@Schema(description = "제목") String title,
	@Schema(description = "상단 고정") boolean pinned,
	@Schema(description = "게시 시각") OffsetDateTime publishedAt) {

	public static NoticeListResponse from(NoticeDto dto) {
		return new NoticeListResponse(dto.id(), dto.title(), dto.pinned(), ApiTimes.of(dto.publishedAt()));
	}
}
