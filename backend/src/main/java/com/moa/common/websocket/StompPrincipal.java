package com.moa.common.websocket;

import java.security.Principal;

import com.moa.common.security.AuthPrincipal;

/**
 * WebSocket 세션의 로그인 사용자. 이름은 사용자 ID라 {@code convertAndSendToUser("7", ...)}로 그 사용자에게 보낸다.
 */
public record StompPrincipal(AuthPrincipal principal) implements Principal {

	@Override
	public String getName() {
		return String.valueOf(principal.userId());
	}
}
