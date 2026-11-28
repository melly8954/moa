package com.moa.api.party;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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
 * 멤버가 나가고 파티가 이어지거나 끝난다 (specs/008-party US6-AC1~AC5, FR-010).
 * 방장이 바뀐 알림(US6-AC6)은 PartyNotificationAcceptanceTest, 탈퇴(US6-AC7·AC8)는 PartyWithdrawalAcceptanceTest가 본다.
 */
class PartyLeaveAcceptanceTest extends AcceptanceTest {

	private static final String PARTIES = "/api/v1/parties";
	private static final long HOST = 10L;
	private static final long FIRST = 11L;
	private static final long SECOND = 12L;
	private static final long APPLICANT = 13L;

	@Autowired
	private CodeSeed codeSeed;

	@Autowired
	private PartyFixtures partyFixtures;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private String host;
	private String first;
	private String second;

	@BeforeEach
	void setUp() {
		codeSeed.restore();
		host = member(HOST);
		first = member(FIRST);
		second = member(SECOND);
	}

	@Test
	@DisplayName("US6-AC1 방장이 아닌 멤버가 나가면 204이고 멤버 목록에서 빠지며 인원이 1 줄어든다")
	void memberLeaves() {
		long partyId = create(host, "OPEN", 4);
		assertThat(join(first, partyId)).hasStatus(200);
		assertThat(join(second, partyId)).hasStatus(200);

		assertThat(leave(first, partyId)).hasStatus(204);

		String detail = body(get(host, PARTIES + "/" + partyId));
		assertThat((Integer)JsonPath.read(detail, "$.memberCount")).isEqualTo(2);
		assertThat(ids(detail, "$.members[*].userId")).containsExactly(HOST, SECOND);
		assertThat(memberRows(partyId, FIRST)).isZero();
	}

	@Test
	@DisplayName("US6-AC2 정원이 차서 자동 마감된 파티에서 한 명이 나가면 다시 모집 중이 되어 모집 페이지에 보인다")
	void autoClosedReopens() {
		long partyId = create(host, "OPEN", 2);
		assertThat(join(first, partyId)).hasStatus(200);
		assertThat(statusOf(partyId)).isEqualTo("CLOSED");
		assertThat(recruitIds()).doesNotContain(partyId);

		assertThat(leave(first, partyId)).hasStatus(204);

		String detail = body(get(host, PARTIES + "/" + partyId));
		assertThat((String)JsonPath.read(detail, "$.status")).isEqualTo("RECRUITING");
		assertThat((Boolean)JsonPath.read(detail, "$.closedByHost")).isFalse();
		assertThat(recruitIds()).contains(partyId);
	}

	@Test
	@DisplayName("US6-AC3 방장이 나가면 joined_at이 가장 이른 멤버가 HOST가 되고 host_user_id도 바뀐다. 같은 시각이면 id가 작은 쪽이다")
	void hostLeavesEarliestTakesOver() {
		long partyId = create(host, "OPEN", 4);
		assertThat(join(first, partyId)).hasStatus(200);
		assertThat(join(second, partyId)).hasStatus(200);
		// 나중에 들어온 SECOND가 더 이른 시각을 갖게 해, 멤버 행 id가 아니라 들어온 시각을 보는지 확인한다
		LocalDateTime earlier = LocalDateTime.now().minusHours(1);
		jdbcTemplate.update("update tb_party_member set joined_at = ? where user_id = ?", earlier, SECOND);

		assertThat(leave(host, partyId)).hasStatus(204);

		assertThat(roleOf(SECOND)).isEqualTo("HOST");
		assertThat(roleOf(FIRST)).isEqualTo("MEMBER");
		assertThat(hostOf(partyId)).isEqualTo(SECOND);
		String detail = body(get(second, PARTIES + "/" + partyId));
		assertThat(((Number)JsonPath.read(detail, "$.host.userId")).longValue()).isEqualTo(SECOND);
		assertThat((String)JsonPath.read(detail, "$.me.relation")).isEqualTo("HOST");

		// 같은 시각이면 멤버 행 id가 작은 쪽(먼저 넣은 쪽)이 잇는다
		long tieHostId = 20L;
		long tieFirst = 21L;
		long tieSecond = 22L;
		String tieHost = member(tieHostId);
		long tieParty = create(tieHost, "OPEN", 4);
		assertThat(join(member(tieFirst), tieParty)).hasStatus(200);
		assertThat(join(member(tieSecond), tieParty)).hasStatus(200);
		LocalDateTime same = LocalDateTime.now().minusMinutes(5);
		jdbcTemplate.update("update tb_party_member set joined_at = ? where user_id in (?, ?)", same, tieFirst,
			tieSecond);

		assertThat(leave(tieHost, tieParty)).hasStatus(204);

		assertThat(hostOf(tieParty)).isEqualTo(tieFirst);
		assertThat(roleOf(tieFirst)).isEqualTo("HOST");
		assertThat(roleOf(tieSecond)).isEqualTo("MEMBER");
	}

	@Test
	@DisplayName("US6-AC4 마지막 멤버가 나가면 파티가 해산되어 상세가 404 R001이고 모집 페이지에 없으며 대기 신청도 사라진다")
	void lastMemberDissolves() {
		long partyId = create(host, "APPROVAL", 3);
		String applicant = member(APPLICANT);
		assertThat(post(applicant, PARTIES + "/" + partyId + "/join-requests", "{}")).hasStatus(201);

		assertThat(leave(host, partyId)).hasStatus(204);

		assertCode(get(first, PARTIES + "/" + partyId), 404, "R001");
		assertThat(recruitIds()).doesNotContain(partyId);
		assertThat(jdbcTemplate.queryForObject("select count(*) from tb_party_join_request where party_id = ?",
			Long.class, partyId)).isZero();
		assertThat(jdbcTemplate.queryForObject("select deleted_at is not null from tb_party where id = ?",
			Boolean.class, partyId)).isTrue();
		assertThat(JsonPath.<List<Object>>read(body(get(applicant, PARTIES + "/me")), "$.joinRequests")).isEmpty();
	}

	@Test
	@DisplayName("US6-AC5 스스로 나간 회원은 모집 중인 그 파티에 다시 참가하거나(공개) 신청할 수 있다(승인)")
	void leftMemberMayReturn() {
		long open = create(host, "OPEN", 4);
		assertThat(join(first, open)).hasStatus(200);
		assertThat(leave(first, open)).hasStatus(204);

		assertThat(join(first, open)).hasStatus(200);
		assertThat(memberRows(open, FIRST)).isOne();

		long approvalHostId = 30L;
		String approvalHost = member(approvalHostId);
		long approval = create(approvalHost, "APPROVAL", 4);
		String code = JsonPath.read(body(get(approvalHost, PARTIES + "/" + approval)), "$.invite.code");
		assertThat(post(second, PARTIES + "/invites/" + code + "/members", "{}")).hasStatus(200);
		assertThat(leave(second, approval)).hasStatus(204);

		assertThat(post(second, PARTIES + "/" + approval + "/join-requests", "{}")).hasStatus(201);
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

	private MvcTestResult leave(String bearer, long partyId) {
		return mvc.delete().uri(PARTIES + "/" + partyId + "/members/me").header(HttpHeaders.AUTHORIZATION, bearer)
			.exchange();
	}

	private List<Long> recruitIds() {
		return ids(body(mvc.get().uri(PARTIES + "").exchange()), "$.content[*].id");
	}

	private String statusOf(long partyId) {
		return jdbcTemplate.queryForObject("select status from tb_party where id = ?", String.class, partyId);
	}

	private long hostOf(long partyId) {
		return jdbcTemplate.queryForObject("select host_user_id from tb_party where id = ?", Long.class, partyId);
	}

	private String roleOf(long userId) {
		return jdbcTemplate.queryForObject("select role from tb_party_member where user_id = ?", String.class, userId);
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
