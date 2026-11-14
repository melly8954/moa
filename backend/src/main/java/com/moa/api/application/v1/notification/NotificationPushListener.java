package com.moa.api.application.v1.notification;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.moa.api.dto.notification.NotificationDto;
import com.moa.api.dto.notification.PushSubscriptionDto;
import com.moa.api.service.notification.NotificationService;
import com.moa.common.helper.webpush.WebPushResult;
import com.moa.common.helper.webpush.WebPushSender;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

/**
 * 새 개인 알림을 그 사용자가 켜 둔 브라우저마다 웹 푸시로 보낸다 (specs/007-notifications US4, research R4·R6).
 * 커밋 뒤 따로(비동기) 보내고, 푸시 서버가 만료를 알린 구독은 지운다. 화면이 보이면 서비스 워커가 띄우지 않는다.
 */
@Component
@RequiredArgsConstructor
public class NotificationPushListener {

	private final NotificationService notificationService;
	private final WebPushSender webPushSender;
	private final ObjectMapper objectMapper;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onCreated(NotificationCreatedEvent event) {
		if (!webPushSender.enabled()) {
			return;
		}
		NotificationDto notification = event.notification();
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("id", notification.id());
		payload.put("title", notification.title());
		payload.put("link", notification.link());
		String json = objectMapper.writeValueAsString(payload);
		for (PushSubscriptionDto subscription : notificationService.getSubscriptions(notification.userId())) {
			WebPushResult result = webPushSender.send(subscription.endpoint(), subscription.p256dh(),
				subscription.auth(), json);
			if (result == WebPushResult.EXPIRED) {
				notificationService.deleteSubscription(subscription.endpoint());
			}
		}
	}
}
