package com.moa.api.service.notification;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moa.api.dto.notification.NotificationDto;
import com.moa.api.dto.notification.NotificationType;
import com.moa.api.dto.notification.PushSubscriptionDto;
import com.moa.api.entity.notification.Notification;
import com.moa.api.entity.notification.PushSubscription;
import com.moa.api.repository.reader.notification.NotificationReader;
import com.moa.api.repository.writer.notification.NotificationWriter;
import com.moa.common.exception.ErrorCode;
import com.moa.common.exception.ServiceException;

import lombok.RequiredArgsConstructor;

/**
 * 개인 알림과 브라우저 알림 구독 (specs/007-notifications). 늘 자기 것만 다룬다.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

	private final NotificationReader notificationReader;
	private final NotificationWriter notificationWriter;

	/**
	 * 개인 알림을 만든다.
	 *
	 * @throws ServiceException 링크가 moa 화면 경로(/로 시작)가 아니면 V001
	 */
	public NotificationDto create(Long userId, NotificationType type, String title, String link) {
		if (link == null || !link.startsWith("/") || link.startsWith("//")) {
			throw new ServiceException(ErrorCode.INVALID_INPUT, "알림 링크는 /로 시작하는 화면 경로여야 합니다: " + link);
		}
		return NotificationDto.from(
			notificationWriter.save(new Notification(userId, type, title, link, LocalDateTime.now())));
	}

	public Page<NotificationDto> getPage(Long userId, Pageable pageable) {
		return notificationReader.findPage(userId, pageable).map(NotificationDto::from);
	}

	public long countUnread(Long userId) {
		return notificationReader.countUnread(userId);
	}

	/**
	 * @throws ServiceException 내 알림이 아니거나 없으면 R001
	 */
	public void read(Long userId, Long id) {
		notificationReader.findMine(id, userId).orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND))
			.read(LocalDateTime.now());
	}

	public void readAll(Long userId) {
		notificationWriter.readAll(userId, LocalDateTime.now());
	}

	/** 보관 기간이 지난 알림을 지운다. 지운 수를 돌려준다 */
	public int deleteOlderThan(int retentionDays) {
		return notificationWriter.deleteCreatedBefore(LocalDateTime.now().minusDays(retentionDays));
	}

	/** 이 브라우저 구독을 내 것으로 저장한다. 같은 endpoint가 있으면 내 것으로 바꾼다 */
	public void subscribe(Long userId, String endpoint, String p256dh, String auth) {
		notificationReader.findSubscription(endpoint).ifPresentOrElse(
			subscription -> subscription.rebind(userId, p256dh, auth),
			() -> notificationWriter.saveSubscription(
				new PushSubscription(userId, endpoint, p256dh, auth, LocalDateTime.now())));
	}

	/** 내 구독이면 지운다. 없거나 남의 것이면 그대로 둔다 */
	public void unsubscribe(Long userId, String endpoint) {
		notificationReader.findSubscription(endpoint)
			.filter(subscription -> subscription.getUserId().equals(userId))
			.ifPresent(notificationWriter::deleteSubscription);
	}

	public List<PushSubscriptionDto> getSubscriptions(Long userId) {
		return notificationReader.findSubscriptions(userId).stream().map(PushSubscriptionDto::from).toList();
	}

	/** 만료된 구독을 지운다 (푸시 서버가 404·410을 돌려줬을 때) */
	public void deleteSubscription(String endpoint) {
		notificationReader.findSubscription(endpoint).ifPresent(notificationWriter::deleteSubscription);
	}
}
