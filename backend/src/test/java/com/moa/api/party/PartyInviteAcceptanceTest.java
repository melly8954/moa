package com.moa.api.party;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

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
 * 초대 코드·링크로 들어오기 (specs/008-party US4, contracts/party-api.md "참가와 나가기").
 * 초대 링크는 화면 경로 /invite/{code}이고 서버에는 코드로 온다. 링크로 들어오기(AC2)와 코드 입력(AC3)은 같은 호출이라,
 * AC3은 소문자로 입력한 코드로 본다 (대소문자를 가리지 않는다).
 */
class PartyInviteAcceptanceTest extends AcceptanceTest {

	private static final String PARTIES = "/api/v1/parties";
	private static final long HOST = 10L;
	private static final long GUEST = 11L;
	private static final long OTHER = 12L;

	@Autowired
	private CodeSeed codeSeed;

	@Autowired
	private PartyFixtures partyFixtures;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private String host;
	private String guest;
	private String other;

	@BeforeEach
	void setUp() {
		codeSeed.restore();
		host = member(HOST);
		guest = member(GUEST);
		other = member(OTHER);
	}

	@Test
	@DisplayName("US4-AC1 멤버가 파티를 열면 초대 코드(8자)와 초대 링크(/invite/{code})가 보인다")
	void memberSeesInvite() {
		long partyId = create(host, "OPEN", 4);
		assertThat(join(guest, partyId)).hasStatus(200);

		for (String viewer : new String[] {host, guest}) {
			String body = body(get(viewer, PARTIES + "/" + partyId));
			String code = JsonPath.read(body, "$.invite.code");
			assertThat(code).hasSize(8).matches("[A-HJ-NP-Z2-9]{8}");
			assertThat((String)JsonPath.read(body, "$.invite.url")).isEqualTo("/invite/" + code);
		}
	}

	@Test
	@DisplayName("US4-AC2 자리가 남은 비공개 파티의 초대 링크로 들어오면 곧바로 멤버가 된다")
	void privateByLink() {
		long partyId = create(host, "PRIVATE", 3);

		MvcTestResult result = joinByInvite(guest, inviteCode(host, partyId));

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$.id").isEqualTo((int)partyId);
		assertThat(result).bodyJson().extractingPath("$.memberCount").isEqualTo(2);
		assertThat(result).bodyJson().extractingPath("$.me.relation").isEqualTo("MEMBER");
		assertThat(memberRows(partyId, GUEST)).isOne();
	}

	@Test
	@DisplayName("US4-AC3 초대 코드를 입력해 들어오면 곧바로 멤버가 된다. 코드는 대소문자를 가리지 않는다")
	void byCode() {
		long partyId = create(host, "OPEN", 3);

		MvcTestResult result = joinByInvite(guest, inviteCode(host, partyId).toLowerCase());

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$.me.relation").isEqualTo("MEMBER");
		assertThat(memberRows(partyId, GUEST)).isOne();
	}

	@Test
	@DisplayName("US4-AC4 승인 파티의 초대 링크로 들어오면 신청 없이 곧바로 멤버가 된다")
	void approvalPartyWithoutRequest() {
		long partyId = create(host, "APPROVAL", 3);

		MvcTestResult result = joinByInvite(guest, inviteCode(host, partyId));

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$.me.relation").isEqualTo("MEMBER");
		assertThat(memberRows(partyId, GUEST)).isOne();
		assertThat(jdbcTemplate.queryForObject("select count(*) from tb_party_join_request where user_id = ?",
			Long.class, GUEST)).isZero();
	}

	@Test
	@DisplayName("US4-AC5 없는 코드와 해산된 파티의 코드로 들어오면 404 B107이다")
	void invalidOrDissolvedCode() {
		assertCode(joinByInvite(guest, "ZZZZZZZZ"), 404, "B107");

		long partyId = create(host, "PRIVATE", 3);
		String code = inviteCode(host, partyId);
		assertThat(delete(host, PARTIES + "/" + partyId)).hasStatus(204);

		assertCode(joinByInvite(guest, code), 404, "B107");
		assertThat(memberRows(partyId, GUEST)).isZero();
	}

	@Test
	@DisplayName("US4-AC6 방장이 초대 코드를 새로 만들면 이전 코드는 404 B107이고 새 코드로는 들어온다")
	void regeneratedCode() {
		long partyId = create(host, "PRIVATE", 3);
		String before = inviteCode(host, partyId);

		MvcTestResult regenerated = post(host, PARTIES + "/" + partyId + "/invite-code", "");
		assertThat(regenerated).hasStatus(200);
		String after = JsonPath.read(body(regenerated), "$.code");
		assertThat(after).isNotEqualTo(before);
		assertThat((String)JsonPath.read(body(regenerated), "$.url")).isEqualTo("/invite/" + after);

		assertCode(joinByInvite(guest, before), 404, "B107");
		assertThat(joinByInvite(guest, after)).hasStatus(200);
	}

	@Test
	@DisplayName("US4-AC7 정원이 찬 파티의 초대 코드는 409 B101, 방장이 마감한 파티의 초대 코드는 409 B102다")
	void fullOrClosed() {
		long full = create(host, "PRIVATE", 2);
		String fullCode = inviteCode(host, full);
		assertThat(joinByInvite(guest, fullCode)).hasStatus(200);

		assertCode(joinByInvite(other, fullCode), 409, "B101");

		long closedHostId = 13L;
		String closedHost = member(closedHostId);
		long closed = create(closedHost, "PRIVATE", 4);
		assertThat(post(closedHost, PARTIES + "/" + closed + "/close", "")).hasStatus(200);

		assertCode(joinByInvite(other, inviteCode(closedHost, closed)), 409, "B102");
		assertThat(memberRows(closed, OTHER)).isZero();
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

	private String inviteCode(String hostBearer, long partyId) {
		return JsonPath.read(body(get(hostBearer, PARTIES + "/" + partyId)), "$.invite.code");
	}

	private MvcTestResult join(String bearer, long partyId) {
		return post(bearer, PARTIES + "/" + partyId + "/members", "{}");
	}

	private MvcTestResult joinByInvite(String bearer, String code) {
		return post(bearer, PARTIES + "/invites/" + code + "/members", "{}");
	}

	private long memberRows(long partyId, long userId) {
		return jdbcTemplate.queryForObject("select count(*) from tb_party_member where party_id = ? and user_id = ?",
			Long.class, partyId, userId);
	}

	private MvcTestResult get(String bearer, String uri) {
		return mvc.get().uri(uri).header(HttpHeaders.AUTHORIZATION, bearer).exchange();
	}

	private MvcTestResult post(String bearer, String uri, String json) {
		return mvc.post().uri(uri).header(HttpHeaders.AUTHORIZATION, bearer).contentType(MediaType.APPLICATION_JSON)
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
