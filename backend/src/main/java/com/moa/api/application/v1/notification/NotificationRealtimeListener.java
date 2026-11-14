package com.moa.api.application.v1.notification;

import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.moa.api.dto.notification.NotificationDto;

import lombok.RequiredArgsConstructor;

/**
 * 새 개인 알림을 그 사용자의 연결된 모든 화면에 보낸다 (specs/007-notifications US3, contracts/realtime.md).
 * 구독 경로 /user/queue/notifications. 커밋 뒤에 보낸다.
 */
@Component
@RequiredArgsConstructor
public class NotificationRealtimeListener {

	public static final String USER_DESTINATION = "/queue/notifications";

	private final SimpMessagingTemplate messagingTemplate;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onCreated(NotificationCreatedEvent event) {
		NotificationDto notification = event.notification();
		Map<String, Object> message = new LinkedHashMap<>();
		message.put("type", "NOTIFICATION_CREATED");
		message.put("id", notification.id());
		message.put("notificationType", notification.type().name());
		message.put("title", notification.title());
		message.put("link", notification.link());
		message.put("createdAt", notification.createdAt().atZone(ZoneId.systemDefault()).toOffsetDateTime().toString());
		messagingTemplate.convertAndSendToUser(String.valueOf(notification.userId()), USER_DESTINATION,
			(Object)message);
	}
}
