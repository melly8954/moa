package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;
import com.moa.api.application.v1.notification.NotificationApplication;
import com.moa.api.dto.notification.NotificationType;
import com.moa.api.scheduler.NotificationCleaner;
import com.moa.support.FakePushServer;
import com.moa.support.RealtimeAcceptanceTest;

import nl.martijndwars.webpush.Utils;

/**
 * 알림. 스펙 specs/007-notifications의 수용 시나리오다 (contracts/notifications-api.md, contracts/realtime.md).
 * 화면에서만 보이는 AC(토스트, 권한 안내)는 e2e/notifications.spec.ts, 브라우저 알림 표시는 화면 직접 확인으로 둔다.
 * US1-AC1은 006-notices US4-AC1과 같은 동작이다.
 */
class NotificationsAcceptanceTest extends RealtimeAcceptanceTest {

	private static final String NOTIFICATIONS = "/api/v1/notifications";
	private static final String SUBSCRIPTIONS = NOTIFICATIONS + "/push-subscriptions";
	private static final long MEMBER = 10L;
	private static final long OTHER = 11L;
	private static final FakePushServer PUSH;
	private static final KeyPair VAPID;

	static {
		try {
			Security.addProvider(new BouncyCastleProvider());
			VAPID = keyPair();
			PUSH = new FakePushServer();
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	/** 테스트 안에서 만든 VAPID 키로 웹 푸시를 켠다 (원칙 VI: 실제 키를 쓰지 않는다) */
	@DynamicPropertySource
	static void webPush(DynamicPropertyRegistry registry) {
		Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
		registry.add("app.web-push.public-key", () -> b64.encodeToString(Utils.encode((ECPublicKey)VAPID.getPublic())));
		registry.add("app.web-push.private-key",
			() -> b64.encodeToString(Utils.encode((ECPrivateKey)VAPID.getPrivate())));
		registry.add("app.web-push.subject", () -> "mailto:test@example.com");
	}

	@AfterAll
	static void stopPushServer() {
		PUSH.close();
	}

	@Autowired
	private NotificationApplication notificationApplication;

	@Autowired
	private NotificationCleaner notificationCleaner;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("US1-AC1 공지를 게시하면 화면을 보고 있는 로그인 사용자에게 공지 제목이 실시간으로 간다")
	void noticePublishedReachesViewers() throws Exception {
		StompSession session = connected(MEMBER, "USER");
		BlockingQueue<Map<String, Object>> notices = subscribe(session, "/topic/notices");

		long id = createNotice("서버 점검 안내", true);

		Map<String, Object> message = next(notices);
		assertThat(message).isNotNull();
		assertThat(message).containsEntry("type", "NOTICE_PUBLISHED").containsEntry("title", "서버 점검 안내")
			.containsEntry("link", "/notices/" + id);
		session.disconnect();
	}

	@Test
	@DisplayName("US1-AC2 공지를 임시 저장하거나 이미 게시한 공지를 고치면 알림이 오지 않는다")
	void draftOrModifySendsNothing() throws Exception {
		long published = createNotice("게시", true);
		StompSession session = connected(MEMBER, "USER");
		BlockingQueue<Map<String, Object>> notices = subscribe(session, "/topic/notices");

		createNotice("임시", false);
		assertThat(asAdmin(mvc.put().uri("/api/v1/admin/notices/" + published).contentType(MediaType.APPLICATION_JSON)
			.content("{\"title\":\"고친 제목\",\"content\":\"<p>x</p>\"}"))).hasStatus(200);

		assertThat(quiet(notices)).isTrue();
		session.disconnect();
	}

	@Test
	@DisplayName("US1-AC3 로그인하지 않은 사람은 실시간 연결을 할 수 없어 알림을 받지 않는다")
	void anonymousCannotConnect() {
		assertThatThrownBy(() -> connect(null).get(5, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class);
	}

	@Test
	@DisplayName("US2-AC1 안 읽은 개인 알림이 3개면 안 읽은 수가 3이다")
	void unreadCount() {
		for (int i = 0; i < 3; i++) {
			notify(MEMBER, "알림 " + i);
		}

		assertThat(asMember(mvc.get().uri(NOTIFICATIONS + "/unread-count"))).hasStatus(200)
			.bodyJson().extractingPath("$.count").isEqualTo(3);
	}

	@Test
	@DisplayName("US2-AC2 벨을 펼치면 최근 알림 10개가 최신순으로 제목·시각·읽음 여부와 함께 온다")
	void recentTen() {
		for (int i = 1; i <= 12; i++) {
			notify(MEMBER, "알림 " + i);
		}

		MvcTestResult page = asMember(mvc.get().uri(NOTIFICATIONS + "?size=10"));

		assertThat(page).hasStatus(200);
		List<String> titles = JsonPath.read(body(page), "$.content[*].title");
		assertThat(titles).hasSize(10).first().isEqualTo("알림 12");
		assertThat(titles).last().isEqualTo("알림 3");
		assertThat(page).bodyJson().extractingPath("$.content[0].read").isEqualTo(false);
		assertThat(page).bodyJson().extractingPath("$.content[0].createdAt").isNotNull();
		assertThat(page).bodyJson().extractingPath("$.totalElements").isEqualTo(12);
	}

	@Test
	@DisplayName("US2-AC3 알림을 누르면 읽음이 되고 안 읽은 수가 하나 준다")
	void readOne() {
		long id = notify(MEMBER, "첫째");
		notify(MEMBER, "둘째");

		assertThat(asMember(mvc.put().uri(NOTIFICATIONS + "/" + id + "/read"))).hasStatus(204);

		assertThat(asMember(mvc.get().uri(NOTIFICATIONS + "/unread-count")))
			.bodyJson().extractingPath("$.count").isEqualTo(1);
		MvcTestResult page = asMember(mvc.get().uri(NOTIFICATIONS));
		List<Boolean> read = JsonPath.read(body(page), "$.content[?(@.title == '첫째')].read");
		assertThat(read).containsExactly(true);
	}

	@Test
	@DisplayName("US2-AC4 모두 읽음을 누르면 모든 알림이 읽음이 되고 안 읽은 수가 0이다")
	void readAll() {
		for (int i = 0; i < 3; i++) {
			notify(MEMBER, "알림 " + i);
		}

		assertThat(asMember(mvc.put().uri(NOTIFICATIONS + "/read-all"))).hasStatus(204);

		assertThat(asMember(mvc.get().uri(NOTIFICATIONS + "/unread-count")))
			.bodyJson().extractingPath("$.count").isEqualTo(0);
	}

	@Test
	@DisplayName("US2-AC5 알림 화면에는 자기 개인 알림만 보이고 남의 알림은 읽음으로 바꿀 수 없다(R001)")
	void onlyMine() {
		notify(MEMBER, "내 알림");
		long others = notify(OTHER, "남의 알림");

		List<String> titles = JsonPath.read(body(asMember(mvc.get().uri(NOTIFICATIONS))), "$.content[*].title");
		assertThat(titles).containsExactly("내 알림");
		assertThat(asMember(mvc.put().uri(NOTIFICATIONS + "/" + others + "/read"))).hasStatus(404)
			.bodyJson().extractingPath("$.code").isEqualTo("R001");
	}

	@Test
	@DisplayName("US2-AC6 로그인하지 않으면 알림함을 쓸 수 없다(A001)")
	void anonymousIsUnauthorized() {
		for (MockMvcRequestBuilder request : List.of(mvc.get().uri(NOTIFICATIONS),
			mvc.get().uri(NOTIFICATIONS + "/unread-count"), mvc.put().uri(NOTIFICATIONS + "/1/read"),
			mvc.put().uri(NOTIFICATIONS + "/read-all"))) {
			assertThat(request.exchange()).hasStatus(401).bodyJson().extractingPath("$.code").isEqualTo("A001");
		}
	}

	@Test
	@DisplayName("US3-AC1 화면을 보고 있는 사용자에게 개인 알림이 생기면 실시간으로 간다")
	void personalNotificationRealtime() throws Exception {
		StompSession session = connected(MEMBER, "USER");
		BlockingQueue<Map<String, Object>> mine = subscribe(session, "/user/queue/notifications");

		long id = notify(MEMBER, "새 알림");

		Map<String, Object> message = next(mine);
		assertThat(message).isNotNull();
		assertThat(message).containsEntry("type", "NOTIFICATION_CREATED").containsEntry("title", "새 알림")
			.containsEntry("link", "/notices").containsEntry("notificationType", "SYSTEM");
		assertThat(((Number)message.get("id")).longValue()).isEqualTo(id);
		session.disconnect();
	}

	@Test
	@DisplayName("US3-AC2 한 사용자에게 생긴 개인 알림은 다른 사용자에게 가지 않는다")
	void personalNotificationIsPrivate() throws Exception {
		StompSession memberSession = connected(MEMBER, "USER");
		StompSession otherSession = connected(OTHER, "USER");
		BlockingQueue<Map<String, Object>> mine = subscribe(memberSession, "/user/queue/notifications");
		BlockingQueue<Map<String, Object>> others = subscribe(otherSession, "/user/queue/notifications");

		notify(MEMBER, "나에게만");

		assertThat(next(mine)).isNotNull();
		assertThat(quiet(others)).isTrue();
		memberSession.disconnect();
		otherSession.disconnect();
	}

	@Test
	@DisplayName("US3-AC3 같은 사용자가 화면을 둘 열어 두면 둘 다 받는다")
	void bothScreensReceive() throws Exception {
		StompSession first = connected(MEMBER, "USER");
		StompSession second = connected(MEMBER, "USER");
		BlockingQueue<Map<String, Object>> firstQueue = subscribe(first, "/user/queue/notifications");
		BlockingQueue<Map<String, Object>> secondQueue = subscribe(second, "/user/queue/notifications");

		notify(MEMBER, "두 화면에");

		assertThat(next(firstQueue)).isNotNull();
		assertThat(next(secondQueue)).isNotNull();
		first.disconnect();
		second.disconnect();
	}

	@Test
	@DisplayName("US4-AC1 브라우저 알림을 켠 사용자에게 개인 알림이 생기면 그 브라우저로 웹 푸시가 간다")
	void pushToSubscribedBrowser() throws Exception {
		assertThat(subscribePush(MEMBER, "browser-a")).hasStatus(204);

		notify(MEMBER, "브라우저로");

		assertThat(PUSH.next()).isEqualTo("POST /browser-a aes128gcm");
	}

	@Test
	@DisplayName("US4-AC3 브라우저 알림을 끄면 그 브라우저로 웹 푸시가 가지 않는다")
	void noPushAfterUnsubscribe() throws Exception {
		assertThat(subscribePush(MEMBER, "browser-b")).hasStatus(204);
		assertThat(asMember(mvc.delete().uri(SUBSCRIPTIONS + "?endpoint=" + PUSH.endpoint("browser-b"))))
			.hasStatus(204);

		notify(MEMBER, "보내지 않음");

		assertThat(PUSH.quiet()).isTrue();
	}

	@Test
	@DisplayName("US4-AC5 로그아웃하면(구독을 지운 뒤) 그 브라우저로 웹 푸시가 가지 않는다")
	void noPushAfterSignOut() throws Exception {
		assertThat(subscribePush(MEMBER, "browser-c")).hasStatus(204);
		// 로그아웃 단추는 서버 로그아웃 전에 그 브라우저의 구독을 지운다 (research R8)
		assertThat(asMember(mvc.delete().uri(SUBSCRIPTIONS + "?endpoint=" + PUSH.endpoint("browser-c"))))
			.hasStatus(204);
		assertThat(mvc.post().uri("/api/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer(MEMBER, "USER"))
			.exchange()).hasStatus2xxSuccessful();

		notify(MEMBER, "로그아웃 뒤");

		assertThat(PUSH.quiet()).isTrue();
	}

	@Test
	@DisplayName("FR-009 보관 기간(90일)이 지난 개인 알림은 정리기가 지운다")
	void cleanerDeletesExpired() {
		insertNotification(MEMBER, "91일 전", LocalDateTime.now().minusDays(91));
		insertNotification(MEMBER, "89일 전", LocalDateTime.now().minusDays(89));

		notificationCleaner.deleteExpired();

		List<String> titles = jdbcTemplate.queryForList("select title from notifications", String.class);
		assertThat(titles).containsExactly("89일 전");
	}

	private long notify(long userId, String title) {
		return notificationApplication.notify(userId, NotificationType.SYSTEM, title, "/notices").id();
	}

	private long createNotice(String title, boolean publish) {
		MvcTestResult created = asAdmin(mvc.post().uri("/api/v1/admin/notices").contentType(MediaType.APPLICATION_JSON)
			.content("{\"title\":\"" + title + "\",\"content\":\"<p>" + title + "</p>\",\"pinned\":false,\"publish\":"
				+ publish + "}"));
		assertThat(created).hasStatus(201);
		Number id = JsonPath.read(body(created), "$.id");
		return id.longValue();
	}

	private MvcTestResult subscribePush(long userId, String browser) throws Exception {
		KeyPair keys = keyPair();
		byte[] auth = new byte[16];
		new SecureRandom().nextBytes(auth);
		Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
		String body = "{\"endpoint\":\"" + PUSH.endpoint(browser) + "\",\"keys\":{\"p256dh\":\""
			+ b64.encodeToString(Utils.encode((ECPublicKey)keys.getPublic())) + "\",\"auth\":\""
			+ b64.encodeToString(auth) + "\"}}";
		return mvc.post().uri(SUBSCRIPTIONS).contentType(MediaType.APPLICATION_JSON).content(body)
			.header(HttpHeaders.AUTHORIZATION, bearer(userId, "USER")).exchange();
	}

	private void insertNotification(long userId, String title, LocalDateTime createdAt) {
		jdbcTemplate.update("insert into notifications (user_id, type, title, link, created_at) values (?, ?, ?, ?, ?)",
			userId, "SYSTEM", title, "/notices", createdAt);
	}

	private MvcTestResult asAdmin(MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(2L, "ADMIN")).exchange();
	}

	private MvcTestResult asMember(MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(MEMBER, "USER")).exchange();
	}

	private static String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static KeyPair keyPair() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("ECDH", "BC");
		generator.initialize(new ECGenParameterSpec("prime256v1"));
		return generator.generateKeyPair();
	}
}
