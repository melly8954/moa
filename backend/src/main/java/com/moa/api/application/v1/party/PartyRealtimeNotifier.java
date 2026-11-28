package com.moa.api.application.v1.party;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 파티 실시간 이벤트를 보낸다 (contracts/party-realtime.md). 커밋 뒤에 보낸다 (007 알림 리스너와 같은 방식).
 * 모양은 {@code {type, partyId, payload, occurredAt}}이다.
 */
@Component
public class PartyRealtimeNotifier {

	public static final String TOPIC_PREFIX = "/topic/parties/";
	public static final String USER_DESTINATION = "/queue/parties";

	private final SimpMessagingTemplate messagingTemplate;
	private final MessageChannel brokerChannel;
	private final PartySubscriptionRegistry subscriptionRegistry;

	public PartyRealtimeNotifier(SimpMessagingTemplate messagingTemplate,
		@Qualifier("brokerChannel") MessageChannel brokerChannel, PartySubscriptionRegistry subscriptionRegistry) {
		this.messagingTemplate = messagingTemplate;
		this.brokerChannel = brokerChannel;
		this.subscriptionRegistry = subscriptionRegistry;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void on(PartyRealtimeEvent event) {
		Map<String, Object> message = new LinkedHashMap<>();
		message.put("type", event.type());
		message.put("partyId", event.partyId());
		message.put("payload", event.payload());
		message.put("occurredAt", OffsetDateTime.now().toString());
		if (event.toUserId() == null) {
			messagingTemplate.convertAndSend(TOPIC_PREFIX + event.partyId(), (Object)message);
		} else {
			messagingTemplate.convertAndSendToUser(String.valueOf(event.toUserId()), USER_DESTINATION,
				(Object)message);
		}
		if (event.unsubscribeUserId() != null) {
			unsubscribe(event.unsubscribeUserId(), event.partyId());
		}
	}

	/** 브로커에 UNSUBSCRIBE를 보내 그 구독을 지운다. 연결과 다른 구독(알림)은 그대로다 */
	private void unsubscribe(Long userId, Long partyId) {
		for (PartySubscriptionRegistry.Subscription subscription : subscriptionRegistry.takeAll(userId, partyId)) {
			SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.UNSUBSCRIBE);
			accessor.setSessionId(subscription.sessionId());
			accessor.setSubscriptionId(subscription.subscriptionId());
			accessor.setLeaveMutable(true);
			brokerChannel.send(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));
		}
	}
}
