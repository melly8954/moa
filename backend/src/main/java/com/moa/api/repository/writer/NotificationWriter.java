package com.moa.api.repository.writer;

import org.springframework.stereotype.Component;

import com.moa.api.entity.Notification;
import com.moa.api.repository.jpa.NotificationRepository;

import lombok.RequiredArgsConstructor;

/**
 * 알림 변경.
 */
@Component
@RequiredArgsConstructor
public class NotificationWriter {

	private final NotificationRepository notificationRepository;

	public Notification create(Notification notification) {
		return notificationRepository.save(notification);
	}
}
