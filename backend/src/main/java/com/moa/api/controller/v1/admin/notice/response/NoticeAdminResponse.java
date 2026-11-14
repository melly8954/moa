package com.moa.api.controller.v1.admin.notice.response;

import java.time.OffsetDateTime;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.notice.NoticeDto;
import com.moa.api.dto.notice.NoticeStatus;
import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공지 (관리)")
public record NoticeAdminResponse(
	@Schema(description = "공지 ID") Long id,
	@Schema(description = "제목") String title,
	@Schema(description = "본문 HTML (저장 때 걸렀다)") String content,
	@Schema(description = "상태") NoticeStatus status,
	@Schema(description = "상단 고정") boolean pinned,
	@Schema(description = "게시 시각. 임시 저장이면 없다") @Nullable OffsetDateTime publishedAt,
	@Schema(description = "만든 시각") OffsetDateTime createdAt,
	@Schema(description = "고친 시각") OffsetDateTime updatedAt) {

	public static NoticeAdminResponse from(NoticeDto dto) {
		return new NoticeAdminResponse(dto.id(), dto.title(), dto.content(), dto.status(), dto.pinned(),
			ApiTimes.of(dto.publishedAt()), ApiTimes.of(dto.createdAt()), ApiTimes.of(dto.updatedAt()));
	}
}
