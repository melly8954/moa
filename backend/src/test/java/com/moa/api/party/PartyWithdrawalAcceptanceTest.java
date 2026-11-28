package com.moa.api.party;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;
import com.moa.api.application.v1.user.AccountRecoveryApplication;
import com.moa.api.application.v1.user.MemberWithdrawalApplication;
import com.moa.api.scheduler.PartyWithdrawalCleaner;
import com.moa.common.security.recovery.RecoveryTicket;
import com.moa.support.AcceptanceTest;
import com.moa.support.CodeSeed;
import com.moa.support.PartyFixtures;

/**
 * 회원 탈퇴와 파티 (specs/008-party US6-AC7·AC8, FR-031, research.md R15).
 * 탈퇴는 MemberWithdrawalApplication으로 한다. 파티 정리는 탈퇴가 커밋된 뒤 같은 스레드의 리스너가 하므로, 부른 뒤 바로 결과를 본다.
 * 탈퇴 API와 복구 쿠키 흐름은 MemberWithdrawalAcceptanceTest가 본다. 여기서는 복구도 Application을 바로 부른다.
 */
class PartyWithdrawalAcceptanceTest extends AcceptanceTest {

	private static final String PARTIES = "/api/v1/parties";
	private static final long HOST = 10L;
	private static final long FIRST = 11L;
	private static final long SECOND = 12L;
	private static final long APPLICANT = 13L;
	private static final long OTHER_HOST = 14L;

	@Autowired
	private CodeSeed codeSeed;

	@Autowired
	private PartyFixtures partyFixtures;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MemberWithdrawalApplication memberWithdrawalApplication;

	@Autowired
	private AccountRecoveryApplication accountRecoveryApplication;

	@Autowired
	private PartyWithdrawalCleaner partyWithdrawalCleaner;

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
	@DisplayName("US6-AC7 방장이 탈퇴하면 가장 먼저 들어온 멤버가 HOST가 되고 새 방장에게만 PARTY_HOST_CHANGED가 하나 생긴다. "
		+ "나감 안내의 이름은 \"탈퇴한 회원\"이고, 복구해도 파티로 돌아오지 않는다")
	void hostWithdraws() {
		long partyId = create(host, "OPEN", 4);
		assertThat(join(first, partyId)).hasStatus(200);
		assertThat(join(second, partyId)).hasStatus(200);

		memberWithdrawalApplication.withdraw(HOST, PartyFixtures.displayName(HOST));

		assertThat(jdbcTemplate.queryForObject("select host_user_id from tb_party where id = ?", Long.class, partyId))
			.isEqualTo(FIRST);
		assertThat(roleOf(FIRST)).isEqualTo("HOST");
		assertThat(memberRows(HOST)).isZero();
		assertThat(notifications(FIRST, "PARTY_HOST_CHANGED")).isOne();
		assertThat(notifications(SECOND, "PARTY_HOST_CHANGED")).isZero();
		assertThat(notifications(HOST, "PARTY_HOST_CHANGED")).isZero();

		String messages = body(get(first, PARTIES + "/" + partyId + "/messages"));
		List<Map<String, Object>> left = JsonPath.read(messages, "$.items[?(@.systemEvent == 'LEFT')]");
		assertThat(left).hasSize(1);
		assertThat(JsonPath.<Number>read(left.getFirst(), "$.subject.userId").longValue()).isEqualTo(HOST);
		assertThat(JsonPath.<String>read(left.getFirst(), "$.subject.displayName")).isEqualTo("탈퇴한 회원");

		LocalDateTime withdrawnAt = jdbcTemplate.queryForObject("select withdrawn_at from tb_user where id = ?",
			LocalDateTime.class, HOST);
		accountRecoveryApplication.recover(Optional.of(new RecoveryTicket(HOST, withdrawnAt)));

		assertThat(jdbcTemplate.queryForObject("select withdrawn_at from tb_user where id = ?", LocalDateTime.class,
			HOST)).isNull();
		assertThat(memberRows(HOST)).isZero();
		MvcTestResult mine = get(host, PARTIES + "/me");
		assertThat(mine).hasStatus(200);
		assertThat(mine).bodyJson().extractingPath("$.party").isNull();
	}

	@Test
	@DisplayName("US6-AC8 승인 파티 둘에 신청해 둔 회원이 탈퇴하면 두 신청이 CANCELLED가 되어 방장의 목록에서 빠지고 아무에게도 알림이 생기지 않는다")
	void applicantWithdraws() {
		String otherHost = member(OTHER_HOST);
		String applicant = member(APPLICANT);
		long one = create(host, "APPROVAL", 3);
		long two = create(otherHost, "APPROVAL", 3);
		assertThat(apply(applicant, one)).hasStatus(201);
		assertThat(apply(applicant, two)).hasStatus(201);
		long notificationsBefore = allNotifications();

		memberWithdrawalApplication.withdraw(APPLICANT, PartyFixtures.displayName(APPLICANT));

		assertThat(jdbcTemplate.queryForList("select status from tb_party_join_request where user_id = ?",
			String.class, APPLICANT)).containsExactly("CANCELLED", "CANCELLED");
		assertThat(JsonPath.<List<Object>>read(body(get(host, PARTIES + "/" + one + "/join-requests")), "$"))
			.isEmpty();
		assertThat(JsonPath.<List<Object>>read(body(get(otherHost, PARTIES + "/" + two + "/join-requests")), "$"))
			.isEmpty();
		assertThat(allNotifications()).isEqualTo(notificationsBefore);
	}

	@Test
	@DisplayName("FR-031 리스너가 처리하지 못해 멤버 행과 대기 신청이 남은 탈퇴 회원을 정리기가 정리한다")
	void cleanerRemovesLeftovers() {
		long partyId = create(host, "OPEN", 4);
		assertThat(join(first, partyId)).hasStatus(200);
		String otherHost = member(OTHER_HOST);
		String applicant = member(APPLICANT);
		long approval = create(otherHost, "APPROVAL", 3);
		assertThat(apply(applicant, approval)).hasStatus(201);
		// 탈퇴 이벤트 없이 탈퇴 상태만 만들어 리스너를 건너뛴다 (실패한 것과 같은 상태)
		jdbcTemplate.update("update tb_user set withdrawn_at = ? where id in (?, ?)", LocalDateTime.now(), FIRST,
			APPLICANT);
		assertThat(memberRows(FIRST)).isOne();

		partyWithdrawalCleaner.cleanUp();

		assertThat(memberRows(FIRST)).isZero();
		assertThat(ids(body(get(host, PARTIES + "/" + partyId)), "$.members[*].userId")).containsExactly(HOST);
		assertThat(jdbcTemplate.queryForList("select status from tb_party_join_request where user_id = ?",
			String.class, APPLICANT)).containsExactly("CANCELLED");
		assertThat(JsonPath.<List<Object>>read(body(get(host, PARTIES + "/" + partyId + "/messages")),
			"$.items[?(@.systemEvent == 'LEFT')]")).hasSize(1);
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

	private String roleOf(long userId) {
		return jdbcTemplate.queryForObject("select role from tb_party_member where user_id = ?", String.class, userId);
	}

	private long memberRows(long userId) {
		return jdbcTemplate.queryForObject("select count(*) from tb_party_member where user_id = ?", Long.class,
			userId);
	}

	private long notifications(long userId, String type) {
		return jdbcTemplate.queryForObject("select count(*) from tb_notification where user_id = ? and type = ?",
			Long.class, userId, type);
	}

	private long allNotifications() {
		return jdbcTemplate.queryForObject("select count(*) from tb_notification", Long.class);
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

	private static String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
