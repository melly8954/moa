package com.moa.api.party;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;
import com.moa.common.security.Role;
import com.moa.support.CodeSeed;
import com.moa.support.PartyFixtures;
import com.moa.support.RealtimeAcceptanceTest;

/**
 * 파티 실시간의 전달 쪽. 스펙 specs/008-party US9의 수용 시나리오다 (contracts/party-realtime.md).
 * 저장은 PartyChatAcceptanceTest가 보고, 여기서는 STOMP 구독으로 이벤트가 오는지 본다. 토큰 없는 CONNECT는
 * 키트 WebSocketAcceptanceTest가 이미 본다.
 */
class PartyRealtimeAcceptanceTest extends RealtimeAcceptanceTest {

	private static final String PARTIES = "/api/v1/parties";
	private static final String MY_QUEUE = "/user/queue/parties";
	private static final long HOST = 10L;
	private static final long MEMBER = 11L;
	private static final long JOINER = 20L;

	@Autowired
	private CodeSeed codeSeed;

	@Autowired
	private PartyFixtures partyFixtures;

	@BeforeEach
	void restoreCodes() {
		codeSeed.restore();
	}

	@Test
	@DisplayName("US9-AC1 한 멤버가 메시지를 보내면 다른 멤버의 파티 구독에 MESSAGE가 1초 안에 온다")
	void messageIsDelivered() throws Exception {
		long partyId = partyFixtures.openParty(HOST, 4);
		partyFixtures.addMember(partyId, MEMBER);
		StompSession session = connected(MEMBER, Role.USER.name());
		BlockingQueue<Map<String, Object>> topic = subscribe(session, topic(partyId));

		assertThat(send(HOST, partyId, "안녕")).hasStatus(201);

		Map<String, Object> event = topic.poll(1, TimeUnit.SECONDS);
		assertThat(event).isNotNull();
		assertThat(event).containsEntry("type", "MESSAGE").containsEntry("partyId", (int)partyId);
		assertThat(event).containsKey("occurredAt");
		Map<String, Object> payload = payload(event);
		assertThat(payload).containsEntry("type", "CHAT").containsEntry("content", "안녕");
		assertThat(payload(payload, "sender")).containsEntry("userId", (int)HOST)
			.containsEntry("displayName", PartyFixtures.displayName(HOST));
		assertThat(payload).containsKey("sentAt");
		session.disconnect();
	}

	@Test
	@DisplayName("US9-AC3 다른 회원이 참가하면 멤버의 파티 구독에 SYSTEM JOINED 안내가 MESSAGE로 온다")
	void joinedNoticeIsDelivered() throws Exception {
		long partyId = partyFixtures.openParty(HOST, 4);
		partyFixtures.member(JOINER);
		StompSession session = connected(HOST, Role.USER.name());
		BlockingQueue<Map<String, Object>> topic = subscribe(session, topic(partyId));

		assertThat(join(JOINER, partyId)).hasStatus(200);

		Map<String, Object> payload = payload(nextOfType(topic, "MESSAGE"));
		assertThat(payload).containsEntry("type", "SYSTEM").containsEntry("systemEvent", "JOINED");
		assertThat(payload(payload, "subject")).containsEntry("userId", (int)JOINER);
		session.disconnect();
	}

	@Test
	@DisplayName("US9-AC7 누가 참가하면 파티를 보고 있는 두 멤버 모두에게 MEMBERS_CHANGED가 바뀐 멤버 목록과 함께 온다")
	void membersChangedIsDeliveredToAll() throws Exception {
		long partyId = partyFixtures.openParty(HOST, 4);
		partyFixtures.addMember(partyId, MEMBER);
		partyFixtures.member(JOINER);
		StompSession hostSession = connected(HOST, Role.USER.name());
		StompSession memberSession = connected(MEMBER, Role.USER.name());
		BlockingQueue<Map<String, Object>> hostTopic = subscribe(hostSession, topic(partyId));
		BlockingQueue<Map<String, Object>> memberTopic = subscribe(memberSession, topic(partyId));

		assertThat(join(JOINER, partyId)).hasStatus(200);

		for (BlockingQueue<Map<String, Object>> topic : List.of(hostTopic, memberTopic)) {
			Map<String, Object> payload = payload(nextOfType(topic, "MEMBERS_CHANGED"));
			assertThat(payload).containsEntry("memberCount", 3).containsEntry("capacity", 4)
				.containsEntry("hostUserId", (int)HOST).containsEntry("status", "RECRUITING");
			assertThat(JsonPath.<List<Integer>>read(payload, "$.members[*].userId"))
				.containsExactly((int)HOST, (int)MEMBER, (int)JOINER);
			assertThat(payload).containsKey("positions");
		}
		hostSession.disconnect();
		memberSession.disconnect();
	}

	@Test
	@DisplayName("US9-AC8 내보내진 멤버의 개인 주소에 KICKED가 오고 파티 구독이 끊긴다. 방장이 해산하면 남은 멤버에게 PARTY_DISSOLVED가 온다")
	void kickedAndDissolvedAreDelivered() throws Exception {
		long partyId = partyFixtures.openParty(HOST, 4);
		partyFixtures.addMembers(partyId, MEMBER, JOINER);
		StompSession kickedSession = connected(JOINER, Role.USER.name());
		StompSession memberSession = connected(MEMBER, Role.USER.name());
		BlockingQueue<Map<String, Object>> kickedTopic = subscribe(kickedSession, topic(partyId));
		BlockingQueue<Map<String, Object>> kickedQueue = subscribe(kickedSession, MY_QUEUE);
		BlockingQueue<Map<String, Object>> memberTopic = subscribe(memberSession, topic(partyId));

		assertThat(mvc.delete().uri(PARTIES + "/" + partyId + "/members/" + JOINER)
			.header(HttpHeaders.AUTHORIZATION, bearer(HOST, Role.USER.name())).exchange()).hasStatus(204);

		Map<String, Object> kicked = nextOfType(kickedQueue, "KICKED");
		assertThat(payload(kicked)).containsEntry("partyId", (int)partyId);
		// 구독을 끊기 전에 보낸 안내와 멤버 변화는 지나간다. 그 뒤 보낸 메시지는 내보내진 회원에게 오지 않는다
		Thread.sleep(300);
		kickedTopic.clear();
		assertThat(send(HOST, partyId, "내보낸 뒤")).hasStatus(201);
		assertThat(payload(nextOfType(memberTopic, "MESSAGE"))).containsEntry("content", "내보낸 뒤");
		assertThat(quiet(kickedTopic)).isTrue();
		assertThat(kickedSession.isConnected()).isTrue();

		memberTopic.clear();
		assertThat(mvc.delete().uri(PARTIES + "/" + partyId)
			.header(HttpHeaders.AUTHORIZATION, bearer(HOST, Role.USER.name())).exchange()).hasStatus(204);

		Map<String, Object> dissolved = nextOfType(memberTopic, "PARTY_DISSOLVED");
		assertThat(dissolved).containsEntry("partyId", (int)partyId);
		kickedSession.disconnect();
		memberSession.disconnect();
	}

	@Test
	@DisplayName("US9-AC9 신청이 들어오거나 취소되거나 처리되면 방장의 개인 주소에 JOIN_REQUESTS_CHANGED가 대기 수와 함께 온다")
	void joinRequestsChangedIsDeliveredToHost() throws Exception {
		long partyId = partyFixtures.approvalParty(HOST, 4);
		partyFixtures.members(JOINER, 21L);
		StompSession hostSession = connected(HOST, Role.USER.name());
		BlockingQueue<Map<String, Object>> hostQueue = subscribe(hostSession, MY_QUEUE);

		assertThat(apply(JOINER, partyId)).hasStatus(201);
		assertThat(pendingCount(hostQueue)).isEqualTo(1);

		assertThat(mvc.delete().uri(PARTIES + "/" + partyId + "/join-requests/me")
			.header(HttpHeaders.AUTHORIZATION, bearer(JOINER, Role.USER.name())).exchange()).hasStatus(204);
		assertThat(pendingCount(hostQueue)).isZero();

		MvcTestResult applied = apply(21L, partyId);
		assertThat(applied).hasStatus(201);
		assertThat(pendingCount(hostQueue)).isEqualTo(1);
		int joinRequestId = JsonPath.read(applied.getResponse().getContentAsString(), "$.joinRequestId");

		assertThat(mvc.post().uri(PARTIES + "/" + partyId + "/join-requests/" + joinRequestId + "/reject")
			.header(HttpHeaders.AUTHORIZATION, bearer(HOST, Role.USER.name())).exchange()).hasStatus(204);
		assertThat(pendingCount(hostQueue)).isZero();
		hostSession.disconnect();
	}

	@Test
	@DisplayName("구독 권한: 멤버가 아닌 회원이 파티 주소를 구독하면 막혀 그 파티의 메시지를 받지 못한다")
	void nonMemberCannotSubscribe() throws Exception {
		long partyId = partyFixtures.openParty(HOST, 4);
		partyFixtures.member(JOINER);
		StompSession outsider = connected(JOINER, Role.USER.name());
		BlockingQueue<Map<String, Object>> topic = subscribe(outsider, topic(partyId));

		assertThat(send(HOST, partyId, "멤버만 봐요")).hasStatus(201);

		assertThat(quiet(topic)).isTrue();
		if (outsider.isConnected()) {
			outsider.disconnect();
		}
	}

	/** 이 종류의 이벤트가 올 때까지 다른 이벤트는 건너뛴다. 5초 안에 오지 않으면 실패한다 */
	private static Map<String, Object> nextOfType(BlockingQueue<Map<String, Object>> queue, String type)
		throws InterruptedException {
		long deadline = System.currentTimeMillis() + 5000;
		while (System.currentTimeMillis() < deadline) {
			Map<String, Object> event = queue.poll(deadline - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
			if (event != null && type.equals(event.get("type"))) {
				return event;
			}
		}
		throw new AssertionError(type + " 이벤트가 오지 않았습니다");
	}

	private static int pendingCount(BlockingQueue<Map<String, Object>> queue) throws InterruptedException {
		return ((Number)payload(nextOfType(queue, "JOIN_REQUESTS_CHANGED")).get("pendingCount")).intValue();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> payload(Map<String, Object> event) {
		return payload(event, "payload");
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> payload(Map<String, Object> map, String key) {
		return (Map<String, Object>)map.get(key);
	}

	private static String topic(long partyId) {
		return "/topic/parties/" + partyId;
	}

	private MvcTestResult send(long userId, long partyId, String content) {
		return mvc.post().uri(PARTIES + "/" + partyId + "/messages")
			.header(HttpHeaders.AUTHORIZATION, bearer(userId, Role.USER.name()))
			.contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"" + content + "\"}").exchange();
	}

	private MvcTestResult join(long userId, long partyId) {
		return mvc.post().uri(PARTIES + "/" + partyId + "/members")
			.header(HttpHeaders.AUTHORIZATION, bearer(userId, Role.USER.name())).exchange();
	}

	private MvcTestResult apply(long userId, long partyId) {
		return mvc.post().uri(PARTIES + "/" + partyId + "/join-requests")
			.header(HttpHeaders.AUTHORIZATION, bearer(userId, Role.USER.name())).exchange();
	}
}
