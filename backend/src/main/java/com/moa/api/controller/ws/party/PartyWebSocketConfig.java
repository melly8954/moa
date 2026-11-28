package com.moa.api.controller.ws.party;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.RequiredArgsConstructor;

/**
 * 파티 구독 검사를 붙인다. 키트 WebSocketConfig는 고치지 않고 설정을 하나 더 둔다 (research.md R3)
 */
@Configuration
@RequiredArgsConstructor
public class PartyWebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final PartySubscriptionInterceptor partySubscriptionInterceptor;

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(partySubscriptionInterceptor);
	}
}
