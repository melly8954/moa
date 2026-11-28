package com.moa.api.party;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;
import com.moa.common.security.Role;
import com.moa.support.AcceptanceTest;
import com.moa.support.CodeSeed;
import com.moa.support.PartyFixtures;

/**
 * 파티 채팅의 API 쪽. 스펙 specs/008-party US9의 수용 시나리오다 (contracts/party-api.md "채팅").
 * 실시간 AC(US9-AC1·AC3)는 여기서 "저장됨"을 보고, "전달됨"은 PartyRealtimeAcceptanceTest가 본다 (plan.md Testing).
 */
class PartyChatAcceptanceTest extends AcceptanceTest {

	private static final String PARTIES = "/api/v1/parties";
	private static final long HOST = 10L;
	private static final long MEMBER = 11L;

	@Autowired
	private CodeSeed codeSeed;

	@Autowired
	private PartyFixtures partyFixtures;

	@BeforeEach
	void restoreCodes() {
		codeSeed.restore();
	}

	@Test
	@DisplayName("US9-AC1 멤버가 메시지를 보내면 201이고 보낸 사람, 내용, 보낸 시각이 저장돼 다른 멤버가 읽는다")
	void sendMessage() {
		long partyId = partyFixtures.openParty(HOST, 4);
		partyFixtures.addMember(partyId, MEMBER);

		MvcTestResult sent = send(HOST, partyId, "안녕하세요");

		assertThat(sent).hasStatus(201);
		assertThat(sent).bodyJson().extractingPath("$.type").isEqualTo("CHAT");
		assertThat(sent).bodyJson().extractingPath("$.sender.userId").isEqualTo((int)HOST);
		assertThat(sent).bodyJson().extractingPath("$.sender.displayName").isEqualTo(PartyFixtures.displayName(HOST));
		assertThat(sent).bodyJson().extractingPath("$.content").isEqualTo("안녕하세요");
		assertThat(sent).bodyJson().extractingPath("$.sentAt").isNotNull();

		MvcTestResult read = messages(MEMBER, partyId, "");
		assertThat(read).hasStatus(200);
		assertThat(read).bodyJson().extractingPath("$.items[*].content").asArray().containsExactly("안녕하세요");
	}

	@Test
	@DisplayName("US9-AC2 새 멤버가 채팅을 열면 마지막 50개를 오래된 순으로 받고 hasMore가 true이며, before로 그 앞 50개를 받는다")
	void readHistoryByCursor() {
		long partyId = partyFixtures.openParty(HOST, 4);
		partyFixtures.chatMessages(partyId, HOST, 120);
		partyFixtures.member(MEMBER);
		assertThat(join(MEMBER, partyId)).hasStatus(200);

		MvcTestResult latest = messages(MEMBER, partyId, "");

		assertThat(latest).hasStatus(200);
		assertThat(latest).bodyJson().extractingPath("$.hasMore").isEqualTo(true);
		List<Integer> latestIds = JsonPath.read(body(latest), "$.items[*].id");
		assertThat(latestIds).hasSize(50).isSorted();
		// 마지막은 들어온 안내(JOINED)이고, 그 앞 49개가 메시지 72~120이다
		assertThat(latest).bodyJson().extractingPath("$.items[49].systemEvent").isEqualTo("JOINED");
		assertThat(latest).bodyJson().extractingPath("$.items[0].content").isEqualTo("메시지 72");
		assertThat(latest).bodyJson().extractingPath("$.items[48].content").isEqualTo("메시지 120");

		MvcTestResult older = messages(MEMBER, partyId, "?before=" + latestIds.getFirst());

		assertThat(older).hasStatus(200);
		assertThat(older).bodyJson().extractingPath("$.hasMore").isEqualTo(true);
		List<Integer> olderIds = JsonPath.read(body(older), "$.items[*].id");
		assertThat(olderIds).hasSize(50).isSorted();
		assertThat(olderIds.getLast()).isLessThan(latestIds.getFirst());
		assertThat(older).bodyJson().extractingPath("$.items[0].content").isEqualTo("메시지 22");
		assertThat(older).bodyJson().extractingPath("$.items[49].content").isEqualTo("메시지 71");
	}

	@Test
	@DisplayName("US9-AC3 참가, 나가기, 내보내기, 방장 바뀜 뒤 채팅에 SYSTEM 안내(JOINED·LEFT·KICKED·HOST_CHANGED)가 저장된다")
	void systemMessagesAreSaved() {
		long partyId = partyFixtures.openParty(HOST, 5);
		partyFixtures.addMember(partyId, MEMBER);
		partyFixtures.members(20L, 21L);

		assertThat(join(20L, partyId)).hasStatus(200);
		assertThat(leave(20L, partyId)).hasStatus(204);
		assertThat(join(21L, partyId)).hasStatus(200);
		assertThat(mvc.delete().uri(PARTIES + "/" + partyId + "/members/21")
			.header(HttpHeaders.AUTHORIZATION, bearer(HOST, Role.USER.name())).exchange()).hasStatus(204);
		assertThat(leave(HOST, partyId)).hasStatus(204);

		assertThat(partyFixtures.systemEvents(partyId))
			.containsExactly("JOINED", "LEFT", "JOINED", "KICKED", "LEFT", "HOST_CHANGED");
		MvcTestResult read = messages(MEMBER, partyId, "");
		assertThat(read).hasStatus(200);
		assertThat(read).bodyJson().extractingPath("$.items[*].type").asArray()
			.containsOnly("SYSTEM");
		assertThat(read).bodyJson().extractingPath("$.items[*].subject.userId").asArray()
			.containsExactly(20, 20, 21, 21, (int)HOST, (int)MEMBER);
	}

	@Test
	@DisplayName("US9-AC4 비멤버, 나간 회원, 내보내진 회원, 대기 신청자가 채팅을 읽거나 보내면 403 A102다")
	void nonMembersCannotChat() {
		long partyId = partyFixtures.approvalParty(HOST, 5);
		partyFixtures.addMembers(partyId, 20L, 21L);
		partyFixtures.member(30L);
		assertThat(leave(20L, partyId)).hasStatus(204);
		assertThat(mvc.delete().uri(PARTIES + "/" + partyId + "/members/21")
			.header(HttpHeaders.AUTHORIZATION, bearer(HOST, Role.USER.name())).exchange()).hasStatus(204);
		partyFixtures.pendingRequest(partyId, 22L);

		for (long outsider : List.of(30L, 20L, 21L, 22L)) {
			assertThat(messages(outsider, partyId, "")).as("읽기 " + outsider).hasStatus(403).bodyJson()
				.extractingPath("$.code").isEqualTo("A102");
			assertThat(send(outsider, partyId, "들여보내 주세요")).as("보내기 " + outsider).hasStatus(403).bodyJson()
				.extractingPath("$.code").isEqualTo("A102");
		}
	}

	@Test
	@DisplayName("US9-AC5 빈 메시지와 500자를 넘는 메시지는 400 V001로 보내지지 않는다")
	void invalidMessagesAreRejected() {
		long partyId = partyFixtures.openParty(HOST, 4);

		assertThat(send(HOST, partyId, "")).hasStatus(400).bodyJson().extractingPath("$.code").isEqualTo("V001");
		assertThat(send(HOST, partyId, "가".repeat(501))).hasStatus(400).bodyJson().extractingPath("$.code")
			.isEqualTo("V001");
		assertThat(send(HOST, partyId, "가".repeat(500))).hasStatus(201);
		assertThat(messages(HOST, partyId, "")).bodyJson().extractingPath("$.items.length()").isEqualTo(1);
	}

	@Test
	@DisplayName("US9-AC6 해산된 파티의 채팅을 이전 멤버가 열면 404 R001이다")
	void dissolvedPartyChatIsNotFound() {
		long partyId = partyFixtures.openParty(HOST, 4);
		partyFixtures.addMember(partyId, MEMBER);
		send(MEMBER, partyId, "곧 해산");
		assertThat(mvc.delete().uri(PARTIES + "/" + partyId)
			.header(HttpHeaders.AUTHORIZATION, bearer(HOST, Role.USER.name())).exchange()).hasStatus(204);

		assertThat(messages(MEMBER, partyId, "")).hasStatus(404).bodyJson().extractingPath("$.code")
			.isEqualTo("R001");
		assertThat(messages(HOST, partyId, "")).hasStatus(404).bodyJson().extractingPath("$.code")
			.isEqualTo("R001");
	}

	@Test
	@DisplayName("FR-018 after로 그 id 뒤의 메시지만 받는다 (다시 연결된 뒤 빠진 메시지 채우기)")
	void readAfterCursor() {
		long partyId = partyFixtures.openParty(HOST, 4);
		long[] ids = partyFixtures.chatMessages(partyId, HOST, 5);

		MvcTestResult after = messages(HOST, partyId, "?after=" + ids[1]);

		assertThat(after).hasStatus(200);
		assertThat(after).bodyJson().extractingPath("$.items[*].id").asArray()
			.containsExactly((int)ids[2], (int)ids[3], (int)ids[4]);
		assertThat(after).bodyJson().extractingPath("$.hasMore").isEqualTo(false);
	}

	private MvcTestResult send(long userId, long partyId, String content) {
		return mvc.post().uri(PARTIES + "/" + partyId + "/messages")
			.header(HttpHeaders.AUTHORIZATION, bearer(userId, Role.USER.name()))
			.contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"" + content + "\"}").exchange();
	}

	private MvcTestResult messages(long userId, long partyId, String query) {
		return mvc.get().uri(PARTIES + "/" + partyId + "/messages" + query)
			.header(HttpHeaders.AUTHORIZATION, bearer(userId, Role.USER.name())).exchange();
	}

	private MvcTestResult join(long userId, long partyId) {
		return mvc.post().uri(PARTIES + "/" + partyId + "/members")
			.header(HttpHeaders.AUTHORIZATION, bearer(userId, Role.USER.name())).exchange();
	}

	private MvcTestResult leave(long userId, long partyId) {
		return mvc.delete().uri(PARTIES + "/" + partyId + "/members/me")
			.header(HttpHeaders.AUTHORIZATION, bearer(userId, Role.USER.name())).exchange();
	}

	private static String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
