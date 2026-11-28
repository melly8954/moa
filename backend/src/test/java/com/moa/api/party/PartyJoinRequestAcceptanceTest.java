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
 * 승인 파티의 참가 신청과 방장의 처리 (specs/008-party US5-AC1~AC8, FR-008, contracts/party-api.md "승인 신청").
 * 개인 알림(US5-AC9~AC11)은 PartyNotificationAcceptanceTest가 본다.
 */
class PartyJoinRequestAcceptanceTest extends AcceptanceTest {

	private static final String PARTIES = "/api/v1/parties";
	private static final long HOST = 10L;
	private static final long APPLICANT = 11L;
	private static final long SECOND = 12L;
	private static final long MEMBER = 13L;

	@Autowired
	private CodeSeed codeSeed;

	@Autowired
	private PartyFixtures partyFixtures;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private String host;
	private String applicant;
	private String second;

	@BeforeEach
	void setUp() {
		codeSeed.restore();
		host = member(HOST);
		applicant = member(APPLICANT);
		second = member(SECOND);
	}

	@Test
	@DisplayName("US5-AC1 자리가 남은 승인 파티에 신청하면 201 PENDING이고 멤버가 되지 않는다")
	void applyStaysPending() {
		long partyId = create(host, "APPROVAL", 3);

		MvcTestResult result = apply(applicant, partyId);

		assertThat(result).hasStatus(201);
		long requestId = ((Number)JsonPath.read(body(result), "$.joinRequestId")).longValue();
		assertThat(status(requestId)).isEqualTo("PENDING");
		String detail = body(get(applicant, PARTIES + "/" + partyId));
		assertThat((Integer)JsonPath.read(detail, "$.memberCount")).isEqualTo(1);
		assertThat((String)JsonPath.read(detail, "$.me.relation")).isEqualTo("PENDING");
		assertThat(((Number)JsonPath.read(detail, "$.me.joinRequestId")).longValue()).isEqualTo(requestId);
	}

	@Test
	@DisplayName("US5-AC2 방장의 신청 목록에 신청한 회원과 신청 시각이 신청순으로 나온다")
	void hostSeesRequestsInOrder() {
		long partyId = create(host, "APPROVAL", 4);
		long first = requestId(apply(applicant, partyId));
		long next = requestId(apply(second, partyId));

		MvcTestResult result = get(host, PARTIES + "/" + partyId + "/join-requests");

		assertThat(result).hasStatus(200);
		String body = body(result);
		assertThat(ids(body, "$[*].joinRequestId")).containsExactly(first, next);
		assertThat(ids(body, "$[*].user.userId")).containsExactly(APPLICANT, SECOND);
		assertThat(JsonPath.<List<String>>read(body, "$[*].user.displayName"))
			.containsExactly(PartyFixtures.displayName(APPLICANT), PartyFixtures.displayName(SECOND));
		assertThat(JsonPath.<List<String>>read(body, "$[*].appliedAt")).allSatisfy(at -> assertThat(at).isNotBlank());
		assertThat(body).doesNotContain("loginId");
	}

	@Test
	@DisplayName("US5-AC3 방장이 승인하면 신청한 회원이 멤버가 되고 인원이 1 늘며, 마지막 자리면 자동 마감된다")
	void approveAdmits() {
		long partyId = create(host, "APPROVAL", 2);
		long requestId = requestId(apply(applicant, partyId));

		MvcTestResult result = approve(host, partyId, requestId);

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$.memberCount").isEqualTo(2);
		assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("CLOSED");
		assertThat(result).bodyJson().extractingPath("$.closedByHost").isEqualTo(false);
		assertThat(ids(body(result), "$.members[*].userId")).containsExactly(HOST, APPLICANT);
		assertThat(status(requestId)).isEqualTo("APPROVED");
	}

	@Test
	@DisplayName("US5-AC4 방장이 거절하면 tb_party_ban에 REJECTED가 남고, 그 회원은 다시 신청하거나 초대로 들어오면 409 B104다")
	void rejectBans() {
		long partyId = create(host, "APPROVAL", 3);
		long requestId = requestId(apply(applicant, partyId));

		assertThat(post(host, PARTIES + "/" + partyId + "/join-requests/" + requestId + "/reject", "")).hasStatus(204);

		assertThat(status(requestId)).isEqualTo("REJECTED");
		assertThat(jdbcTemplate.queryForObject("select reason from tb_party_ban where party_id = ? and user_id = ?",
			String.class, partyId, APPLICANT)).isEqualTo("REJECTED");
		assertCode(apply(applicant, partyId), 409, "B104");
		assertCode(joinByInvite(applicant, inviteCode(host, partyId)), 409, "B104");
		assertThat(memberRows(partyId, APPLICANT)).isZero();
	}

	@Test
	@DisplayName("US5-AC5 정원이 찬 뒤 대기 중인 신청을 승인하면 409 B101이다")
	void approveWhenFull() {
		long partyId = create(host, "APPROVAL", 2);
		long first = requestId(apply(applicant, partyId));
		long next = requestId(apply(second, partyId));
		assertThat(approve(host, partyId, first)).hasStatus(200);

		assertCode(approve(host, partyId, next), 409, "B101");
		assertThat(memberRows(partyId, SECOND)).isZero();
		assertThat(status(next)).isEqualTo("PENDING");
	}

	@Test
	@DisplayName("US5-AC6 신청한 회원이 취소하면 CANCELLED가 되고 다시 신청할 수 있다")
	void cancelThenApplyAgain() {
		long partyId = create(host, "APPROVAL", 3);
		long requestId = requestId(apply(applicant, partyId));

		assertThat(delete(applicant, PARTIES + "/" + partyId + "/join-requests/me")).hasStatus(204);

		assertThat(status(requestId)).isEqualTo("CANCELLED");
		assertThat(JsonPath.<List<Object>>read(body(get(host, PARTIES + "/" + partyId + "/join-requests")), "$"))
			.isEmpty();
		MvcTestResult again = apply(applicant, partyId);
		assertThat(again).hasStatus(201);
		assertThat(status(requestId(again))).isEqualTo("PENDING");
	}

	@Test
	@DisplayName("US5-AC7 대기 중인 신청이 있는데 또 신청하면 409 B108이다")
	void duplicateApply() {
		long partyId = create(host, "APPROVAL", 3);
		assertThat(apply(applicant, partyId)).hasStatus(201);

		assertCode(apply(applicant, partyId), 409, "B108");
		assertThat(jdbcTemplate.queryForObject(
			"select count(*) from tb_party_join_request where party_id = ? and user_id = ?", Long.class, partyId,
			APPLICANT)).isOne();
	}

	@Test
	@DisplayName("US5-AC8 방장이 아닌 회원(멤버, 신청자)이 신청 목록·승인·거절을 하면 403 A101이다")
	void nonHostForbidden() {
		long partyId = create(host, "APPROVAL", 4);
		String member = member(MEMBER);
		assertThat(joinByInvite(member, inviteCode(host, partyId))).hasStatus(200);
		long requestId = requestId(apply(applicant, partyId));

		for (String caller : List.of(member, applicant)) {
			assertCode(get(caller, PARTIES + "/" + partyId + "/join-requests"), 403, "A101");
			assertCode(approve(caller, partyId, requestId), 403, "A101");
			assertCode(post(caller, PARTIES + "/" + partyId + "/join-requests/" + requestId + "/reject", ""), 403,
				"A101");
		}
		assertThat(status(requestId)).isEqualTo("PENDING");
	}

	@Test
	@DisplayName("FR-008 방장이 직접 마감한 파티에서도 자리가 남으면 이미 받은 신청을 승인할 수 있다")
	void approveWhileClosedByHost() {
		long partyId = create(host, "APPROVAL", 3);
		long requestId = requestId(apply(applicant, partyId));
		assertThat(post(host, PARTIES + "/" + partyId + "/close", "")).hasStatus(200);

		MvcTestResult result = approve(host, partyId, requestId);

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$.memberCount").isEqualTo(2);
		assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("CLOSED");
		assertThat(result).bodyJson().extractingPath("$.closedByHost").isEqualTo(true);
	}

	@Test
	@DisplayName("FR-005 공개 파티에 신청하면 409 B112다")
	void applyToOpenParty() {
		long partyId = create(host, "OPEN", 3);

		assertCode(apply(applicant, partyId), 409, "B112");
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

	private MvcTestResult apply(String bearer, long partyId) {
		return post(bearer, PARTIES + "/" + partyId + "/join-requests", "{}");
	}

	private MvcTestResult approve(String bearer, long partyId, long requestId) {
		return post(bearer, PARTIES + "/" + partyId + "/join-requests/" + requestId + "/approve", "");
	}

	private MvcTestResult joinByInvite(String bearer, String code) {
		return post(bearer, PARTIES + "/invites/" + code + "/members", "{}");
	}

	private long requestId(MvcTestResult applied) {
		assertThat(applied).hasStatus(201);
		return ((Number)JsonPath.read(body(applied), "$.joinRequestId")).longValue();
	}

	private String status(long requestId) {
		return jdbcTemplate.queryForObject("select status from tb_party_join_request where id = ?", String.class,
			requestId);
	}

	private long memberRows(long partyId, long userId) {
		return jdbcTemplate.queryForObject("select count(*) from tb_party_member where party_id = ? and user_id = ?",
			Long.class, partyId, userId);
	}

	private static List<Long> ids(String body, String path) {
		List<Number> values = JsonPath.read(body, path);
		return values.stream().map(Number::longValue).toList();
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
