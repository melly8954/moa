package com.moa.api.repository.reader.notification;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.moa.api.entity.notification.Notification;
import com.moa.api.entity.notification.PushSubscription;
import com.moa.api.repository.jpa.notification.NotificationRepository;
import com.moa.api.repository.jpa.notification.PushSubscriptionRepository;

import lombok.RequiredArgsConstructor;

/**
 * 알림 조회 (specs/007-notifications). 늘 받는 사람 기준으로 읽는다.
 */
@Component
@RequiredArgsConstructor
public class NotificationReader {

	private final NotificationRepository notificationRepository;
	private final PushSubscriptionRepository pushSubscriptionRepository;

	/** 내 알림, 최신순 (created_at desc, id desc) */
	public Page<Notification> findPage(Long userId, Pageable pageable) {
		return notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable);
	}

	public long countUnread(Long userId) {
		return notificationRepository.countByUserIdAndReadAtIsNull(userId);
	}

	/** 내 알림 하나. 남의 알림이면 비어 있다 */
	public Optional<Notification> findMine(Long id, Long userId) {
		return notificationRepository.findByIdAndUserId(id, userId);
	}

	public Optional<PushSubscription> findSubscription(String endpoint) {
		return pushSubscriptionRepository.findByEndpoint(endpoint);
	}

	public List<PushSubscription> findSubscriptions(Long userId) {
		return pushSubscriptionRepository.findByUserId(userId);
	}
}
