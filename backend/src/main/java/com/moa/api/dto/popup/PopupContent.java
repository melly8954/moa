package com.moa.api.dto.popup;

import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;

/**
 * 팝업을 만들거나 고칠 때 넣는 칸 (specs/016-banner-popup FR-004). 내용은 걸러진 HTML, 일시는 서버 시간대다.
 */
public record PopupContent(String title, String content, @Nullable String linkUrl, LocalDateTime startsAt,
	@Nullable LocalDateTime endsAt, int sortOrder, boolean enabled) {
}
