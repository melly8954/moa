package com.moa.common.websocket;

import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import com.moa.common.exception.ServiceException;
import com.moa.common.security.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

/**
 * STOMP CONNECT 때 Authorization 헤더의 액세스 토큰을 확인한다. 토큰이 없거나 무효하면 연결을 거절한다.
 * 브라우저 WebSocket은 핸드셰이크에 헤더를 실을 수 없어서, HTTP가 아니라 STOMP CONNECT 프레임에서 인증한다.
 */
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

	private static final String BEARER = "Bearer ";

	private final JwtTokenProvider tokenProvider;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
			return message;
		}
		String header = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith(BEARER)) {
			throw new MessageDeliveryException("로그인이 필요합니다 (A001)");
		}
		try {
			accessor.setUser(new StompPrincipal(tokenProvider.parse(header.substring(BEARER.length()))));
		} catch (ServiceException ex) {
			throw new MessageDeliveryException("토큰이 유효하지 않습니다 (" + ex.getErrorCode().getCode() + ")");
		}
		return message;
	}
}
