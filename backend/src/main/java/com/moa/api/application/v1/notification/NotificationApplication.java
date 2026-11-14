package com.moa.api.application.v1.notification;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.dto.notification.NotificationDto;
import com.moa.api.dto.notification.NotificationType;
import com.moa.api.service.notification.NotificationService;
import com.moa.common.helper.webpush.WebPushSender;

import lombok.RequiredArgsConstructor;

/**
 * 알림 (specs/007-notifications). 다른 기능은 {@link #notify}로 한 사용자에게 개인 알림을 보낸다.
 * 부르는 쪽 트랜잭션에서 저장하고, 커밋 뒤 실시간(/user/queue/notifications)과 웹 푸시로 보낸다.
 */
@Component
@RequiredArgsConstructor
public class NotificationApplication {

	private final NotificationService notificationService;
	private final WebPushSender webPushSender;
	private final ApplicationEventPublisher eventPublisher;

	/**
	 * 한 사용자에게 개인 알림을 보낸다.
	 *
	 * @param link 관련 화면 경로 (/로 시작)
	 */
	@Transactional
	public NotificationDto notify(Long userId, NotificationType type, String title, String link) {
		NotificationDto notification = notificationService.create(userId, type, title, link);
		eventPublisher.publishEvent(new NotificationCreatedEvent(notification));
		return notification;
	}

	@Transactional(readOnly = true)
	public Page<NotificationDto> getPage(Long userId, Pageable pageable) {
		return notificationService.getPage(userId, pageable);
	}

	@Transactional(readOnly = true)
	public long countUnread(Long userId) {
		return notificationService.countUnread(userId);
	}

	@Transactional
	public void read(Long userId, Long id) {
		notificationService.read(userId, id);
	}

	@Transactional
	public void readAll(Long userId) {
		notificationService.readAll(userId);
	}

	/** VAPID 공개 키. 웹 푸시가 꺼져 있으면 null */
	public String pushPublicKey() {
		return webPushSender.publicKey();
	}

	@Transactional
	public void subscribe(Long userId, String endpoint, String p256dh, String auth) {
		notificationService.subscribe(userId, endpoint, p256dh, auth);
	}

	@Transactional
	public void unsubscribe(Long userId, String endpoint) {
		notificationService.unsubscribe(userId, endpoint);
	}

	/** 보관 기간이 지난 알림을 지운다 (정리기가 부른다) */
	@Transactional
	public int deleteExpired(int retentionDays) {
		return notificationService.deleteOlderThan(retentionDays);
	}
}
