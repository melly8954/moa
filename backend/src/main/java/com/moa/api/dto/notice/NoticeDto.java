package com.moa.api.dto.notice;

import java.time.LocalDateTime;

import com.moa.api.entity.notice.Notice;

public record NoticeDto(Long id, String title, String content, NoticeStatus status, boolean pinned,
	LocalDateTime publishedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {

	public static NoticeDto from(Notice notice) {
		return new NoticeDto(notice.getId(), notice.getTitle(), notice.getContent(), notice.getStatus(),
			notice.isPinned(), notice.getPublishedAt(), notice.getCreatedAt(), notice.getUpdatedAt());
	}
}
