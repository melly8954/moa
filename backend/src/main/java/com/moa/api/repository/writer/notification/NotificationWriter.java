package com.moa.api.repository.writer.notification;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.moa.api.entity.notification.Notification;
import com.moa.api.entity.notification.PushSubscription;
import com.moa.api.repository.jpa.notification.NotificationRepository;
import com.moa.api.repository.jpa.notification.PushSubscriptionRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationWriter {

	private final NotificationRepository notificationRepository;
	private final PushSubscriptionRepository pushSubscriptionRepository;

	public Notification save(Notification notification) {
		return notificationRepository.save(notification);
	}

	/** 내 안 읽은 알림을 모두 읽음으로. 바꾼 수를 돌려준다 */
	public int readAll(Long userId, LocalDateTime now) {
		return notificationRepository.readAll(userId, now);
	}

	/** 이 시각보다 먼저 만든 알림을 지운다. 지운 수를 돌려준다 */
	public int deleteCreatedBefore(LocalDateTime cutoff) {
		return notificationRepository.deleteCreatedBefore(cutoff);
	}

	public PushSubscription saveSubscription(PushSubscription subscription) {
		return pushSubscriptionRepository.save(subscription);
	}

	public void deleteSubscription(PushSubscription subscription) {
		pushSubscriptionRepository.delete(subscription);
	}
}
