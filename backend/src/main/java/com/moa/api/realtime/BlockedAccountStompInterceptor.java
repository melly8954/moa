package com.moa.api.realtime;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import com.moa.common.security.access.AccountAccessChecker;
import com.moa.common.websocket.StompPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * 막힌 계정(탈퇴한 회원)의 STOMP CONNECT를 거절한다 (specs/018-member-withdrawal FR-006, research R3).
 * HTTP와 같이 A002로 답해 탈퇴 여부를 알리지 않는다.
 *
 * <p>
 * 키트 {@code StompAuthInterceptor}가 사용자를 정한 다음에 돌아야 한다. 순서는 {@link BlockedAccountInterceptorRegistrar}가 정한다.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class BlockedAccountStompInterceptor implements ChannelInterceptor {

	private final AccountAccessChecker accountAccessChecker;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
			return message;
		}
		if (accessor.getUser() instanceof StompPrincipal user
			&& accountAccessChecker.isBlocked(user.principal().userId())) {
			throw new MessageDeliveryException("토큰이 유효하지 않습니다 (A002)");
		}
		return message;
	}
}
