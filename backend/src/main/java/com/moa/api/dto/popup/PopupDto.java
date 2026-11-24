package com.moa.api.dto.popup;

import java.time.LocalDateTime;

import com.moa.api.dto.marketing.ExposureStatus;
import com.moa.api.entity.popup.Popup;

public record PopupDto(Long id, String title, String content, String linkUrl, LocalDateTime startsAt,
	LocalDateTime endsAt, int sortOrder, boolean enabled, ExposureStatus status, LocalDateTime createdAt,
	LocalDateTime updatedAt) {

	/**
	 * @param now 상태를 정할 지금 시각
	 */
	public static PopupDto from(Popup popup, LocalDateTime now) {
		return new PopupDto(popup.getId(), popup.getTitle(), popup.getContent(), popup.getLinkUrl(),
			popup.getStartsAt(), popup.getEndsAt(), popup.getSortOrder(), popup.isEnabled(),
			ExposureStatus.of(popup.isEnabled(), popup.getStartsAt(), popup.getEndsAt(), now), popup.getCreatedAt(),
			popup.getUpdatedAt());
	}
}
