package com.moa.support;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.jwt.JwtTokenProvider;

/**
 * 실시간(STOMP) 인수 테스트 기반. 실제 포트로 서버를 띄워 STOMP 클라이언트로 붙고, HTTP는 MockMvc로 부른다
 * (키트 WebSocketAcceptanceTest 방식, specs/007-notifications research R11). 태그 db다.
 */
@Tag("db")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
public abstract class RealtimeAcceptanceTest {

	/** 오지 않아야 하는 메시지를 기다리는 시간 */
	protected static final long QUIET_MILLIS = 1500;

	private static final StompSessionHandlerAdapter NO_OP_HANDLER = new StompSessionHandlerAdapter() {
		@Override
		public void handleTransportError(StompSession session, Throwable exception) {
			// 연결 실패는 future가 알린다
		}
	};

	@LocalServerPort
	private int port;

	@Autowired
	protected MockMvcTester mvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private DatabaseCleaner databaseCleaner;

	@AfterEach
	void cleanDatabase() {
		databaseCleaner.clean();
	}

	/** Authorization 헤더 값 */
	protected String bearer(Long userId, String... roles) {
		return "Bearer " + jwtTokenProvider.createAccessToken(new AuthPrincipal(userId, Set.of(roles)));
	}

	/** 토큰으로 연결한다. authorization이 null이면 토큰 없이 */
	protected CompletableFuture<StompSession> connect(String authorization) {
		WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
		client.setMessageConverter(new JacksonJsonMessageConverter());
		StompHeaders connectHeaders = new StompHeaders();
		if (authorization != null) {
			connectHeaders.add(HttpHeaders.AUTHORIZATION, authorization);
		}
		return client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), connectHeaders,
			NO_OP_HANDLER);
	}

	protected StompSession connected(Long userId, String role) throws Exception {
		return connect(bearer(userId, role)).get(5, TimeUnit.SECONDS);
	}

	/** 구독하고 받은 메시지를 쌓는 큐를 돌려준다. 구독 등록은 비동기라 조금 기다린다 */
	protected BlockingQueue<Map<String, Object>> subscribe(StompSession session, String destination)
		throws InterruptedException {
		BlockingQueue<Map<String, Object>> received = new LinkedBlockingQueue<>();
		session.subscribe(destination, new StompFrameHandler() {
			@Override
			public Type getPayloadType(StompHeaders headers) {
				return Map.class;
			}

			@Override
			@SuppressWarnings("unchecked")
			public void handleFrame(StompHeaders headers, Object payload) {
				received.add((Map<String, Object>)payload);
			}
		});
		// 구독 프레임이 서버 브로커에 등록될 시간을 준다 (키트 테스트는 보내기를 되풀이해 같은 문제를 푼다)
		Thread.sleep(300);
		return received;
	}

	/** 메시지 하나를 기다린다. 5초 안에 오지 않으면 null */
	protected static Map<String, Object> next(BlockingQueue<Map<String, Object>> queue) throws InterruptedException {
		return queue.poll(5, TimeUnit.SECONDS);
	}

	/** 일정 시간 동안 아무것도 오지 않았나 */
	protected static boolean quiet(BlockingQueue<Map<String, Object>> queue) throws InterruptedException {
		return queue.poll(QUIET_MILLIS, TimeUnit.MILLISECONDS) == null;
	}
}
