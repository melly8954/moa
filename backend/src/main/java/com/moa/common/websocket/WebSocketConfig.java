package com.moa.common.websocket;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.moa.common.config.CorsProperties;
import com.moa.common.security.PublicPaths;

import lombok.RequiredArgsConstructor;

/**
 * STOMP over WebSocket (선택 헬퍼 websocket).
 *
 * <ul>
 *   <li>연결: {@code /ws}. 허용 출처는 CORS와 같다 (CORS_ALLOWED_ORIGINS)</li>
 *   <li>구독: {@code /topic/...}(여럿에게), {@code /user/queue/...}(로그인한 그 사용자에게만)</li>
 *   <li>보내기: {@code /app/...} → {@code @MessageMapping} 메서드</li>
 * </ul>
 *
 * 브로커는 서버 메모리(simple broker)다. 서버가 여러 대면 다른 서버에 붙은 사용자에게 닿지 않는다 (HELPER.md 주의).
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	public static final String ENDPOINT = "/ws";

	private final CorsProperties corsProperties;
	private final StompAuthInterceptor stompAuthInterceptor;

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint(ENDPOINT).setAllowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new));
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic", "/queue");
		registry.setApplicationDestinationPrefixes("/app");
		registry.setUserDestinationPrefix("/user");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(stompAuthInterceptor);
	}

	/** 핸드셰이크는 HTTP 인증 없이 연다. 로그인은 STOMP CONNECT에서 StompAuthInterceptor가 확인한다 */
	@Bean
	public PublicPaths webSocketPublicPaths() {
		return () -> List.of(ENDPOINT, ENDPOINT + "/**");
	}
}
