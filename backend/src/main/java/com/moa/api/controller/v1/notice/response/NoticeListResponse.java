package com.moa.api.controller.v1.notice.response;

import java.time.OffsetDateTime;

import com.moa.api.dto.notice.PublishedNoticeDto;
import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공지 목록 항목")
public record NoticeListResponse(
	@Schema(description = "공지 ID") Long id,
	@Schema(description = "게시 순번. 처음 게시한 공지가 1, 지운 공지는 세지 않는다") long number,
	@Schema(description = "제목") String title,
	@Schema(description = "상단 고정. 화면은 번호 대신 고정 표시를 보인다") boolean pinned,
	@Schema(description = "게시 시각") OffsetDateTime publishedAt) {

	public static NoticeListResponse from(PublishedNoticeDto dto) {
		return new NoticeListResponse(dto.id(), dto.number(), dto.title(), dto.pinned(),
			ApiTimes.of(dto.publishedAt()));
	}
}
