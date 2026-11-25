package com.moa.api.dto.banner;

import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;

/**
 * 배너를 만들거나 고칠 때 넣는 칸 (specs/016-banner-popup FR-003). 일시는 서버 시간대다.
 */
public record BannerContent(String name, String wideImageUrl, @Nullable String narrowImageUrl, String altText,
	@Nullable String linkUrl, LocalDateTime startsAt, @Nullable LocalDateTime endsAt, int sortOrder,
	boolean enabled) {
}
