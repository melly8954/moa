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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;
import com.moa.support.AcceptanceTest;
import com.moa.support.CodeSeed;
import com.moa.support.PartyFixtures;

/**
 * 방장의 파티 관리 (specs/008-party US7-AC1~AC9, contracts/party-api.md "모집 페이지와 파티").
 * 개인 알림(US7-AC10·AC11)은 PartyNotificationAcceptanceTest가 본다.
 * 신청은 승인 파티에서만, 참가는 공개 파티에서만 되므로, 세 길을 모두 보는 AC는 참가 방식을 바꿔 가며 부른다.
 */
class PartyHostAcceptanceTest extends AcceptanceTest {

	private static final String PARTIES = "/api/v1/parties";
	private static final long HOST = 10L;
	private static final long FIRST = 11L;
	private static final long SECOND = 12L;
	private static final long OUTSIDER = 13L;

	@Autowired
	private CodeSeed codeSeed;

	@Autowired
	private PartyFixtures partyFixtures;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private String host;
	private String first;
	private String second;
	private String outsider;

	@BeforeEach
	void setUp() {
		codeSeed.restore();
		host = member(HOST);
		first = member(FIRST);
		second = member(SECOND);
		outsider = member(OUTSIDER);
	}

	@Test
	@DisplayName("US7-AC1 방장이 게임, 제목, 소개, 인원을 고치면 바뀐 내용이 보이고, 인원을 지금보다 작게 하면 409 B109다")
	void modify() {
		long partyId = create(host, "OPEN", 4);
		assertThat(join(first, partyId)).hasStatus(200);
		assertThat(join(second, partyId)).hasStatus(200);

		MvcTestResult result = put(host, PARTIES + "/" + partyId,
			"{\"gameCode\":\"OVERWATCH\",\"title\":\"새 제목\",\"description\":\"새 소개\",\"capacity\":5}");

		assertThat(result).hasStatus(200);
		String detail = body(get(first, PARTIES + "/" + partyId));
		assertThat(JsonPath.<String>read(detail, "$.gameCode")).isEqualTo("OVERWATCH");
		assertThat(JsonPath.<String>read(detail, "$.title")).isEqualTo("새 제목");
		assertThat(JsonPath.<String>read(detail, "$.description")).isEqualTo("새 소개");
		assertThat(JsonPath.<Integer>read(detail, "$.capacity")).isEqualTo(5);

		assertCode(put(host, PARTIES + "/" + partyId, "{\"gameCode\":\"OVERWATCH\",\"title\":\"새 제목\",\"capacity\":2}"),
			409, "B109");
		assertThat(jdbcTemplate.queryForObject("select capacity from tb_party where id = ?", Integer.class, partyId))
			.isEqualTo(5);
	}

	@Test
	@DisplayName("US7-AC2 공개 파티를 비공개로 바꾸면 모집 페이지에서 빠지고, 멤버가 아니면 상세 404·참가 R001이며 초대로만 들어온다")
	void toPrivate() {
		long partyId = create(host, "OPEN", 4);
		assertThat(recruitIds()).contains(partyId);

		MvcTestResult result = put(host, PARTIES + "/" + partyId + "/join-policy", "{\"joinPolicy\":\"PRIVATE\"}");

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$.joinPolicy").isEqualTo("PRIVATE");
		assertThat(recruitIds()).doesNotContain(partyId);
		assertCode(get(first, PARTIES + "/" + partyId), 404, "R001");
		assertCode(join(first, partyId), 404, "R001");
		assertThat(joinByInvite(first, partyFixtures.inviteCode(partyId))).hasStatus(200);
	}

	@Test
	@DisplayName("US7-AC3 비공개 파티를 공개나 승인으로 바꾸면 모집 중일 때 모집 페이지에 다시 보인다")
	void fromPrivate() {
		long toApproval = create(host, "PRIVATE", 4);
		assertThat(recruitIds()).doesNotContain(toApproval);

		assertThat(put(host, PARTIES + "/" + toApproval + "/join-policy", "{\"joinPolicy\":\"APPROVAL\"}"))
			.hasStatus(200);

		assertThat(recruitIds()).contains(toApproval);

		String otherHost = member(20L);
		long toOpen = create(otherHost, "PRIVATE", 4);
		assertThat(put(otherHost, PARTIES + "/" + toOpen + "/join-policy", "{\"joinPolicy\":\"OPEN\"}"))
			.hasStatus(200);

		assertThat(recruitIds()).contains(toOpen);
		assertThat(join(first, toOpen)).hasStatus(200);
	}

	@Test
	@DisplayName("US7-AC4 대기 신청이 있는 승인 파티를 공개나 비공개로 바꾸면 대기 신청이 모두 CANCELLED가 된다")
	void approvalChangeCancelsRequests() {
		long toOpen = create(host, "APPROVAL", 4);
		long firstRequest = requestId(apply(first, toOpen));
		long secondRequest = requestId(apply(second, toOpen));

		assertThat(put(host, PARTIES + "/" + toOpen + "/join-policy", "{\"joinPolicy\":\"OPEN\"}")).hasStatus(200);

		assertThat(partyFixtures.requestStatus(firstRequest)).isEqualTo("CANCELLED");
		assertThat(partyFixtures.requestStatus(secondRequest)).isEqualTo("CANCELLED");

		String otherHost = member(20L);
		long toPrivate = create(otherHost, "APPROVAL", 4);
		long outsiderRequest = requestId(apply(outsider, toPrivate));

		assertThat(put(otherHost, PARTIES + "/" + toPrivate + "/join-policy", "{\"joinPolicy\":\"PRIVATE\"}"))
			.hasStatus(200);

		assertThat(partyFixtures.requestStatus(outsiderRequest)).isEqualTo("CANCELLED");
	}

	@Test
	@DisplayName("US7-AC5 내보내기는 204이고 tb_party_ban에 KICKED가 남으며, 그 회원은 참가·신청·초대 입장이 409 B104다. 자기 자신은 409 B113이다")
	void kick() {
		long partyId = create(host, "OPEN", 4);
		assertThat(join(first, partyId)).hasStatus(200);

		assertThat(delete(host, PARTIES + "/" + partyId + "/members/" + FIRST)).hasStatus(204);

		assertThat(partyFixtures.isInAnyParty(FIRST)).isFalse();
		assertThat(jdbcTemplate.queryForObject("select reason from tb_party_ban where party_id = ? and user_id = ?",
			String.class, partyId, FIRST)).isEqualTo("KICKED");
		assertCode(join(first, partyId), 409, "B104");
		assertCode(joinByInvite(first, partyFixtures.inviteCode(partyId)), 409, "B104");
		assertThat(put(host, PARTIES + "/" + partyId + "/join-policy", "{\"joinPolicy\":\"APPROVAL\"}"))
			.hasStatus(200);
		assertCode(apply(first, partyId), 409, "B104");

		assertCode(delete(host, PARTIES + "/" + partyId + "/members/" + HOST), 409, "B113");
		assertThat(partyFixtures.memberCount(partyId)).isEqualTo(1);
	}

	@Test
	@DisplayName("US7-AC6 마감하면 자리가 남아도 모집 페이지에서 빠지고 참가·신청·초대 입장이 409 B102이며, 다시 열면 돌아온다. 정원이 찼는데 다시 열면 409 B101이다")
	void closeAndReopen() {
		long partyId = create(host, "OPEN", 3);

		MvcTestResult closed = post(host, PARTIES + "/" + partyId + "/close", "");

		assertThat(closed).hasStatus(200);
		assertThat(closed).bodyJson().extractingPath("$.status").isEqualTo("CLOSED");
		assertThat(closed).bodyJson().extractingPath("$.closedByHost").isEqualTo(true);
		assertThat(recruitIds()).doesNotContain(partyId);
		assertCode(join(first, partyId), 409, "B102");
		assertCode(joinByInvite(first, partyFixtures.inviteCode(partyId)), 409, "B102");
		assertThat(put(host, PARTIES + "/" + partyId + "/join-policy", "{\"joinPolicy\":\"APPROVAL\"}"))
			.hasStatus(200);
		assertCode(apply(first, partyId), 409, "B102");

		MvcTestResult reopened = post(host, PARTIES + "/" + partyId + "/reopen", "");

		assertThat(reopened).hasStatus(200);
		assertThat(reopened).bodyJson().extractingPath("$.status").isEqualTo("RECRUITING");
		assertThat(reopened).bodyJson().extractingPath("$.closedByHost").isEqualTo(false);
		assertThat(recruitIds()).contains(partyId);
		assertThat(apply(first, partyId)).hasStatus(201);

		assertThat(joinByInvite(second, partyFixtures.inviteCode(partyId))).hasStatus(200);
		assertThat(joinByInvite(outsider, partyFixtures.inviteCode(partyId))).hasStatus(200);
		assertThat(partyFixtures.memberCount(partyId)).isEqualTo(3);
		assertCode(post(host, PARTIES + "/" + partyId + "/reopen", ""), 409, "B101");
	}

	@Test
	@DisplayName("US7-AC7 초대 코드를 새로 만들면 새 코드와 링크가 생기고 이전 코드는 404 B107이다")
	void regenerateInviteCode() {
		long partyId = create(host, "PRIVATE", 4);
		String before = partyFixtures.inviteCode(partyId);

		MvcTestResult result = post(host, PARTIES + "/" + partyId + "/invite-code", "");

		assertThat(result).hasStatus(200);
		String after = JsonPath.read(body(result), "$.code");
		assertThat(after).hasSize(8).isNotEqualTo(before);
		assertThat(JsonPath.<String>read(body(result), "$.url")).isEqualTo("/invite/" + after);
		assertCode(joinByInvite(first, before), 404, "B107");
		assertThat(joinByInvite(first, after)).hasStatus(200);
	}

	@Test
	@DisplayName("US7-AC8 해산은 204이고 그 뒤 상세가 404 R001이며 모집 페이지에 없고 대기 신청이 사라진다")
	void dissolve() {
		long partyId = create(host, "APPROVAL", 4);
		assertThat(joinByInvite(first, partyFixtures.inviteCode(partyId))).hasStatus(200);
		assertThat(apply(second, partyId)).hasStatus(201);

		assertThat(delete(host, PARTIES + "/" + partyId)).hasStatus(204);

		assertCode(get(host, PARTIES + "/" + partyId), 404, "R001");
		assertCode(get(first, PARTIES + "/" + partyId), 404, "R001");
		assertThat(recruitIds()).doesNotContain(partyId);
		assertThat(partyFixtures.requestCount(SECOND)).isZero();
		assertThat(partyFixtures.isInAnyParty(FIRST)).isFalse();
		assertThat(partyFixtures.isInAnyParty(HOST)).isFalse();
	}

	@Test
	@DisplayName("US7-AC9 방장이 아닌 멤버가 고치기, 방식 바꾸기, 내보내기, 마감, 재개, 코드 새로 만들기, 해산을 하면 모두 403 A101이다")
	void nonHostForbidden() {
		long partyId = create(host, "OPEN", 4);
		assertThat(join(first, partyId)).hasStatus(200);
		assertThat(join(second, partyId)).hasStatus(200);
		String base = PARTIES + "/" + partyId;

		assertCode(put(first, base, "{\"gameCode\":\"LOL\",\"title\":\"바꿈\",\"capacity\":4}"), 403, "A101");
		assertCode(put(first, base + "/join-policy", "{\"joinPolicy\":\"PRIVATE\"}"), 403, "A101");
		assertCode(delete(first, base + "/members/" + SECOND), 403, "A101");
		assertCode(post(first, base + "/close", ""), 403, "A101");
		assertCode(post(first, base + "/reopen", ""), 403, "A101");
		assertCode(post(first, base + "/invite-code", ""), 403, "A101");
		assertCode(delete(first, base), 403, "A101");

		String detail = body(get(host, base));
		assertThat(JsonPath.<String>read(detail, "$.title")).isEqualTo("같이 할 사람");
		assertThat(JsonPath.<String>read(detail, "$.joinPolicy")).isEqualTo("OPEN");
		assertThat(JsonPath.<String>read(detail, "$.status")).isEqualTo("RECRUITING");
		assertThat(JsonPath.<Integer>read(detail, "$.memberCount")).isEqualTo(3);
	}

	// ---------- 도우미 ----------

	private String member(long userId) {
		partyFixtures.member(userId);
		return bearer(userId, "USER");
	}

	private long create(String bearer, String joinPolicy, int capacity) {
		MvcTestResult result = post(bearer, PARTIES, "{\"gameCode\":\"LOL\",\"title\":\"같이 할 사람\",\"capacity\":"
			+ capacity + ",\"joinPolicy\":\"" + joinPolicy + "\"}");
		assertThat(result).hasStatus(201);
		return ((Number)JsonPath.read(body(result), "$.id")).longValue();
	}

	private MvcTestResult join(String bearer, long partyId) {
		return post(bearer, PARTIES + "/" + partyId + "/members", "{}");
	}

	private MvcTestResult apply(String bearer, long partyId) {
		return post(bearer, PARTIES + "/" + partyId + "/join-requests", "{}");
	}

	private MvcTestResult joinByInvite(String bearer, String code) {
		return post(bearer, PARTIES + "/invites/" + code + "/members", "{}");
	}

	private long requestId(MvcTestResult applied) {
		assertThat(applied).hasStatus(201);
		return ((Number)JsonPath.read(body(applied), "$.joinRequestId")).longValue();
	}

	private List<Long> recruitIds() {
		List<Number> ids = JsonPath.read(body(mvc.get().uri(PARTIES).exchange()), "$.content[*].id");
		return ids.stream().map(Number::longValue).toList();
	}

	private MvcTestResult get(String bearer, String uri) {
		return mvc.get().uri(uri).header(HttpHeaders.AUTHORIZATION, bearer).exchange();
	}

	private MvcTestResult post(String bearer, String uri, String json) {
		return mvc.post().uri(uri).header(HttpHeaders.AUTHORIZATION, bearer).contentType(MediaType.APPLICATION_JSON)
			.content(json).exchange();
	}

	private MvcTestResult put(String bearer, String uri, String json) {
		return mvc.put().uri(uri).header(HttpHeaders.AUTHORIZATION, bearer).contentType(MediaType.APPLICATION_JSON)
			.content(json).exchange();
	}

	private MvcTestResult delete(String bearer, String uri) {
		return mvc.delete().uri(uri).header(HttpHeaders.AUTHORIZATION, bearer).exchange();
	}

	private static void assertCode(MvcTestResult result, int status, String code) {
		assertThat(result).hasStatus(status).bodyJson().extractingPath("$.code").isEqualTo(code);
	}

	private static String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
