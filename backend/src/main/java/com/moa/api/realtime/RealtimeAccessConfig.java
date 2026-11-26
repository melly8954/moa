package com.moa.api.realtime;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

import lombok.RequiredArgsConstructor;

/**
 * 키트 WebSocketConfig에 더하는 moa 설정 (specs/018-member-withdrawal research R3). 키트 파일은 고치지 않는다.
 *
 * 전송: 열리고 닫힌 세션을 {@link RealtimeSessions}에 알린다. 막힌 계정의 CONNECT 거절은 순서 때문에 여기서 붙이지 않는다
 * ({@link BlockedAccountInterceptorRegistrar}).
 */
@Configuration
@RequiredArgsConstructor
public class RealtimeAccessConfig implements WebSocketMessageBrokerConfigurer {

	private final RealtimeSessions realtimeSessions;

	@Override
	public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
		registration.addDecoratorFactory(handler -> new SessionTrackingHandler(handler, realtimeSessions));
	}

	private static final class SessionTrackingHandler extends WebSocketHandlerDecorator {

		private final RealtimeSessions sessions;

		SessionTrackingHandler(WebSocketHandler delegate, RealtimeSessions sessions) {
			super(delegate);
			this.sessions = sessions;
		}

		@Override
		public void afterConnectionEstablished(WebSocketSession session) throws Exception {
			sessions.opened(session);
			super.afterConnectionEstablished(session);
		}

		@Override
		public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
			sessions.closed(session);
			super.afterConnectionClosed(session, closeStatus);
		}
	}
}
