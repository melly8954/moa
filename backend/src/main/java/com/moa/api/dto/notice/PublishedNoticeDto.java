package com.moa.api.dto.notice;

import java.time.LocalDateTime;

/** 사용자 목록 항목. number는 게시 순번이다 (specs/006-notices FR-010) */
public record PublishedNoticeDto(Long id, long number, String title, boolean pinned, LocalDateTime publishedAt) {
}
