package com.moa.api.controller.v1.admin.notice.response;

import java.time.OffsetDateTime;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.notice.NoticeDto;
import com.moa.api.dto.notice.NoticeStatus;
import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공지 관리 목록 항목")
public record NoticeAdminListResponse(
	@Schema(description = "공지 ID") Long id,
	@Schema(description = "제목") String title,
	@Schema(description = "상태") NoticeStatus status,
	@Schema(description = "상단 고정") boolean pinned,
	@Schema(description = "게시 시각. 임시 저장이면 없다") @Nullable OffsetDateTime publishedAt,
	@Schema(description = "고친 시각") OffsetDateTime updatedAt) {

	public static NoticeAdminListResponse from(NoticeDto dto) {
		return new NoticeAdminListResponse(dto.id(), dto.title(), dto.status(), dto.pinned(),
			ApiTimes.of(dto.publishedAt()), ApiTimes.of(dto.updatedAt()));
	}
}
