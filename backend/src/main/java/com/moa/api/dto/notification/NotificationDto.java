package com.moa.api.dto.notification;

import java.time.LocalDateTime;

import com.moa.api.entity.notification.Notification;

public record NotificationDto(Long id, Long userId, NotificationType type, String title, String link,
	LocalDateTime createdAt, boolean read) {

	public static NotificationDto from(Notification notification) {
		return new NotificationDto(notification.getId(), notification.getUserId(), notification.getType(),
			notification.getTitle(), notification.getLink(), notification.getCreatedAt(), notification.isRead());
	}
}
