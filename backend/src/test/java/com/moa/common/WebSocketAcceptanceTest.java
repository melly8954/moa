package com.moa.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Type;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.jwt.JwtTokenProvider;
import com.moa.support.TestcontainersConfig;

/**
 * WebSocket 연결과 사용자별 메시지 (선택 헬퍼 websocket). 실제 포트로 서버를 띄워 STOMP 클라이언트로 붙는다.
 */
@Tag("db")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class WebSocketAcceptanceTest {

	/** 연결 이벤트는 쓰지 않는다. 결과는 connectAsync의 future로 본다 */
	private static final StompSessionHandlerAdapter NO_OP_HANDLER = new StompSessionHandlerAdapter() {
		@Override
		public void handleTransportError(StompSession session, Throwable exception) {
			// 연결 실패는 future가 알린다
		}
	};

	@LocalServerPort
	private int port;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@Test
	@DisplayName("WebSocket: 토큰으로 연결하면 /user/queue로 그 사용자에게만 메시지를 받는다")
	void userQueue() throws Exception {
		String token = jwtTokenProvider.createAccessToken(new AuthPrincipal(7L, Set.of("USER")));
		StompSession session = connect("Bearer " + token).get(5, TimeUnit.SECONDS);
		BlockingQueue<String> received = new LinkedBlockingQueue<>();
		session.subscribe("/user/queue/test", new StompFrameHandler() {
			@Override
			public Type getPayloadType(StompHeaders headers) {
				return String.class;
			}

			@Override
			public void handleFrame(StompHeaders headers, Object payload) {
				received.add((String)payload);
			}
		});

		// 구독 등록은 비동기라, 받을 때까지 몇 번 보낸다
		String message = null;
		for (int i = 0; i < 20 && message == null; i++) {
			messagingTemplate.convertAndSendToUser("7", "/queue/test", "hello");
			message = received.poll(250, TimeUnit.MILLISECONDS);
		}
		assertThat(message).isEqualTo("hello");
		session.disconnect();
	}

	@Test
	@DisplayName("WebSocket: 토큰이 없으면 연결을 거절한다")
	void rejectsWithoutToken() {
		assertThatThrownBy(() -> connect(null).get(5, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class);
	}

	private CompletableFuture<StompSession> connect(String authorization) {
		WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
		client.setMessageConverter(new StringMessageConverter());
		StompHeaders connectHeaders = new StompHeaders();
		if (authorization != null) {
			connectHeaders.add(HttpHeaders.AUTHORIZATION, authorization);
		}
		return client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), connectHeaders,
			NO_OP_HANDLER);
	}
}
