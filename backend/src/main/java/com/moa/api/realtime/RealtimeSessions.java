package com.moa.api.realtime;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import com.moa.common.security.access.AccountAccessChecker;
import com.moa.common.websocket.StompPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 이 서버에 열린 WebSocket 세션을 기억하고 회원의 세션을 닫는다 (specs/018-member-withdrawal FR-006, research R3).
 *
 * <p>
 * 회원과 세션의 연결은 Spring의 {@link SimpUserRegistry}가 안다({@code SessionConnectedEvent}·{@code SessionDisconnectEvent}로
 * 채운다). 여기서는 세션 ID로 닫을 수 있는 {@link WebSocketSession}만 쥔다. 다른 서버의 세션은 닫지 못한다(키트 브로커와 같은 한계).
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeSessions {

	/** 메시지 브로커 설정이 이 빈을 거쳐 만들어지므로(데코레이터) 순환을 피해 늦게 찾는다 */
	private final ObjectProvider<SimpUserRegistry> userRegistry;
	private final AccountAccessChecker accountAccessChecker;

	private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

	void opened(WebSocketSession session) {
		sessions.put(session.getId(), session);
	}

	void closed(WebSocketSession session) {
		sessions.remove(session.getId());
	}

	/** 이 회원의 열린 연결을 모두 닫는다. 닫다가 실패해도 던지지 않는다 */
	public void closeAll(Long userId) {
		SimpUser user = userRegistry.getObject().getUser(String.valueOf(userId));
		if (user == null) {
			return;
		}
		List<String> ids = user.getSessions().stream().map(SimpSession::getId).toList();
		for (String id : ids) {
			close(id, userId);
		}
	}

	/**
	 * 연결이 맺어질 때 한 번 더 확인한다. CONNECT 확인을 지난 직후 탈퇴가 커밋되면, 그 연결은 아직 레지스트리에 없어 closeAll을
	 * 비껴간다. 맺어진 뒤에 막힌 계정이면 닫는다 (specs/018-member-withdrawal FR-006, research R3)
	 */
	@EventListener
	public void onConnected(SessionConnectedEvent event) {
		if (!(event.getUser() instanceof StompPrincipal user)) {
			return;
		}
		Long userId = user.principal().userId();
		String sessionId = SimpMessageHeaderAccessor.getSessionId(event.getMessage().getHeaders());
		if (sessionId != null && accountAccessChecker.isBlocked(userId)) {
			close(sessionId, userId);
		}
	}

	private void close(String sessionId, Long userId) {
		WebSocketSession session = sessions.get(sessionId);
		if (session == null) {
			return;
		}
		try {
			session.close(CloseStatus.POLICY_VIOLATION);
		} catch (IOException | RuntimeException ex) {
			log.warn("실시간 연결을 닫지 못함: userId={}, error={}", userId, ex.getClass().getSimpleName());
		}
	}
}
