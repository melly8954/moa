package com.moa.api.controller.ws.party;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import com.moa.api.application.v1.party.PartyApplication;
import com.moa.api.application.v1.party.PartyRealtimeNotifier;
import com.moa.api.application.v1.party.PartySubscriptionRegistry;

import lombok.RequiredArgsConstructor;

/**
 * 파티 주소 구독은 그 파티의 지금 멤버만 할 수 있다 (specs/008-party research R3, contracts/party-realtime.md).
 * 허락한 구독은 기록해, 멤버에서 빠지면 서버가 끊을 수 있게 한다.
 */
@Component
@RequiredArgsConstructor
public class PartySubscriptionInterceptor implements ChannelInterceptor {

	private static final Pattern PARTY_TOPIC = Pattern.compile("^" + Pattern.quote(PartyRealtimeNotifier.TOPIC_PREFIX)
		+ "(\\d+)$");

	private final PartyApplication partyApplication;
	private final PartySubscriptionRegistry subscriptionRegistry;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null || accessor.getCommand() == null) {
			return message;
		}
		Long userId = userId(accessor.getUser());
		switch (accessor.getCommand()) {
			case SUBSCRIBE -> checkSubscribe(accessor, userId);
			case UNSUBSCRIBE -> {
				if (userId != null) {
					subscriptionRegistry.remove(userId, accessor.getSessionId(), accessor.getSubscriptionId());
				}
			}
			case DISCONNECT -> {
				if (userId != null) {
					subscriptionRegistry.removeSession(userId, accessor.getSessionId());
				}
			}
		}
		return message;
	}

	private void checkSubscribe(StompHeaderAccessor accessor, Long userId) {
		String destination = accessor.getDestination();
		Matcher matcher = destination == null ? null : PARTY_TOPIC.matcher(destination);
		if (matcher == null || !matcher.matches()) {
			return;
		}
		Long partyId = Long.valueOf(matcher.group(1));
		if (userId == null || !partyApplication.isMember(partyId, userId)) {
			throw new MessageDeliveryException("파티 멤버만 구독할 수 있습니다");
		}
		subscriptionRegistry.add(userId, new PartySubscriptionRegistry.Subscription(accessor.getSessionId(),
			accessor.getSubscriptionId(), partyId));
	}

	private static Long userId(Principal user) {
		return user == null ? null : Long.valueOf(user.getName());
	}
}
