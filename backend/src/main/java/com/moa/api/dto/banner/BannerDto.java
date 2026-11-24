package com.moa.api.dto.banner;

import java.time.LocalDateTime;

import com.moa.api.dto.marketing.ExposureStatus;
import com.moa.api.entity.banner.Banner;

public record BannerDto(Long id, String name, String wideImageUrl, String narrowImageUrl, String altText,
	String linkUrl, LocalDateTime startsAt, LocalDateTime endsAt, int sortOrder, boolean enabled,
	ExposureStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {

	/**
	 * @param now 상태를 정할 지금 시각
	 */
	public static BannerDto from(Banner banner, LocalDateTime now) {
		return new BannerDto(banner.getId(), banner.getName(), banner.getWideImageUrl(), banner.getNarrowImageUrl(),
			banner.getAltText(), banner.getLinkUrl(), banner.getStartsAt(), banner.getEndsAt(), banner.getSortOrder(),
			banner.isEnabled(),
			ExposureStatus.of(banner.isEnabled(), banner.getStartsAt(), banner.getEndsAt(), now),
			banner.getCreatedAt(), banner.getUpdatedAt());
	}
}
