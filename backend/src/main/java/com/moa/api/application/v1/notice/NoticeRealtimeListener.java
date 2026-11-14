package com.moa.api.application.v1.notice;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;

/**
 * 공지 게시를 연결된 모두에게 알린다 (specs/007-notifications US1, 006 US4). 구독 경로 /topic/notices.
 * 전체 알림은 저장하지 않는다. 커밋 뒤에 보낸다.
 */
@Component
@RequiredArgsConstructor
public class NoticeRealtimeListener {

	public static final String TOPIC = "/topic/notices";

	private final SimpMessagingTemplate messagingTemplate;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onPublished(NoticePublishedEvent event) {
		Map<String, Object> message = new LinkedHashMap<>();
		message.put("type", "NOTICE_PUBLISHED");
		message.put("id", event.id());
		message.put("title", event.title());
		message.put("link", "/notices/" + event.id());
		messagingTemplate.convertAndSend(TOPIC, (Object)message);
	}
}
