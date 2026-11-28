package com.moa.api.party;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.moa.support.AcceptanceTest;
import com.moa.support.CodeSeed;
import com.moa.support.PartyFixtures;

/**
 * 파티 개인 알림 (specs/008-party FR-029, 007-notifications FR-012).
 * 받는 사람에게 그 종류의 알림이 하나 생기고 일을 한 사람에게는 생기지 않는 것을 tb_notification으로 본다.
 * 토스트·브라우저 알림 모양은 007 테스트가 본다. 제목은 FR-029 표 그대로다 (표시 이름, 「파티 제목」).
 */
class PartyNotificationAcceptanceTest extends AcceptanceTest {

	private static final String PARTIES = "/api/v1/parties";
	private static final String TITLE = "주말 듀오";
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

	@BeforeEach
	void setUp() {
		codeSeed.restore();
		partyFixtures.members(HOST, FIRST, SECOND, APPLICANT);
		host = bearer(HOST, "USER");
	}

	@Test
	@DisplayName("US5-AC9 승인 파티에 신청하면 방장에게 PARTY_APPLIED가 하나 생기고 신청한 회원에게는 생기지 않는다")
	void applied() {
		long partyId = partyFixtures.party(HOST).title(TITLE).joinPolicy("APPROVAL").capacity(3).create();

		assertThat(post(bearer(APPLICANT, "USER"), PARTIES + "/" + partyId + "/join-requests", "{}"))
			.hasStatus(201);

		List<Map<String, Object>> sent = notifications(HOST, "PARTY_APPLIED");
		assertThat(sent).hasSize(1);
		assertThat(sent.getFirst()).containsEntry("title",
			PartyFixtures.displayName(APPLICANT) + "님이 「" + TITLE + "」 파티에 참가를 신청했습니다")
			.containsEntry("link", "/parties/" + partyId);
		assertThat(count(APPLICANT)).isZero();
	}

	@Test
	@DisplayName("US5-AC10 방장이 승인하면 신청한 회원에게 PARTY_APPROVED가 하나 생기고 방장에게는 생기지 않는다")
	void approved() {
		long partyId = partyFixtures.party(HOST).title(TITLE).joinPolicy("APPROVAL").capacity(3).create();
		long requestId = partyFixtures.pendingRequest(partyId, APPLICANT);

		assertThat(post(host, PARTIES + "/" + partyId + "/join-requests/" + requestId + "/approve", ""))
			.hasStatus(200);

		List<Map<String, Object>> sent = notifications(APPLICANT, "PARTY_APPROVED");
		assertThat(sent).hasSize(1);
		assertThat(sent.getFirst()).containsEntry("title", "「" + TITLE + "」 파티 참가가 승인되었습니다")
			.containsEntry("link", "/parties/" + partyId);
		assertThat(count(APPLICANT)).isOne();
		assertThat(count(HOST)).isZero();
	}

	@Test
	@DisplayName("US5-AC11 방장이 거절하면 신청한 회원에게 PARTY_REJECTED가 하나 생기고 연결은 /recruit다")
	void rejected() {
		long partyId = partyFixtures.party(HOST).title(TITLE).joinPolicy("APPROVAL").capacity(3).create();
		long requestId = partyFixtures.pendingRequest(partyId, APPLICANT);

		assertThat(post(host, PARTIES + "/" + partyId + "/join-requests/" + requestId + "/reject", ""))
			.hasStatus(204);

		List<Map<String, Object>> sent = notifications(APPLICANT, "PARTY_REJECTED");
		assertThat(sent).hasSize(1);
		assertThat(sent.getFirst()).containsEntry("title", "「" + TITLE + "」 파티 참가 신청이 받아들여지지 않았습니다")
			.containsEntry("link", "/recruit");
		assertThat(count(HOST)).isZero();
	}

	@Test
	@DisplayName("US6-AC6 방장이 나가면 새 방장에게만 PARTY_HOST_CHANGED가 하나 생기고 나간 회원과 다른 멤버에게는 생기지 않는다")
	void hostChanged() {
		long partyId = partyFixtures.party(HOST).title(TITLE).capacity(4).create();
		partyFixtures.addMembers(partyId, FIRST, SECOND);

		assertThat(mvc.delete().uri(PARTIES + "/" + partyId + "/members/me").header(HttpHeaders.AUTHORIZATION, host)
			.exchange()).hasStatus(204);

		List<Map<String, Object>> sent = notifications(FIRST, "PARTY_HOST_CHANGED");
		assertThat(sent).hasSize(1);
		assertThat(sent.getFirst()).containsEntry("title", "「" + TITLE + "」 파티의 방장이 되었습니다")
			.containsEntry("link", "/parties/" + partyId);
		assertThat(count(FIRST)).isOne();
		assertThat(count(SECOND)).isZero();
		assertThat(count(HOST)).isZero();
	}

	@Test
	@DisplayName("US7-AC10 방장이 내보내면 내보내진 회원에게 PARTY_KICKED가 하나 생기고 연결은 /recruit다. 방장과 다른 멤버에게는 없다")
	void kicked() {
		long partyId = partyFixtures.party(HOST).title(TITLE).capacity(4).create();
		partyFixtures.addMembers(partyId, FIRST, SECOND);

		assertThat(mvc.delete().uri(PARTIES + "/" + partyId + "/members/" + FIRST)
			.header(HttpHeaders.AUTHORIZATION, host).exchange()).hasStatus(204);

		List<Map<String, Object>> sent = notifications(FIRST, "PARTY_KICKED");
		assertThat(sent).hasSize(1);
		assertThat(sent.getFirst()).containsEntry("title", "「" + TITLE + "」 파티에서 내보내졌습니다")
			.containsEntry("link", "/recruit");
		assertThat(count(FIRST)).isOne();
		assertThat(count(SECOND)).isZero();
		assertThat(count(HOST)).isZero();
	}

	@Test
	@DisplayName("US7-AC11 방장이 해산하면 두 멤버와 대기 신청자에게 PARTY_DISSOLVED가 하나씩 생기고 방장에게는 없다")
	void dissolved() {
		long partyId = partyFixtures.party(HOST).title(TITLE).joinPolicy("APPROVAL").capacity(5).create();
		partyFixtures.addMembers(partyId, FIRST, SECOND);
		partyFixtures.pendingRequest(partyId, APPLICANT);

		assertThat(mvc.delete().uri(PARTIES + "/" + partyId).header(HttpHeaders.AUTHORIZATION, host).exchange())
			.hasStatus(204);

		for (long recipient : new long[] {FIRST, SECOND, APPLICANT}) {
			List<Map<String, Object>> sent = notifications(recipient, "PARTY_DISSOLVED");
			assertThat(sent).hasSize(1);
			assertThat(sent.getFirst()).containsEntry("title", "「" + TITLE + "」 파티가 해산되었습니다")
				.containsEntry("link", "/recruit");
			assertThat(count(recipient)).isOne();
		}
		assertThat(count(HOST)).isZero();
	}

	// ---------- 도우미 ----------

	private List<Map<String, Object>> notifications(long userId, String type) {
		return jdbcTemplate.queryForList("select title, link from tb_notification where user_id = ? and type = ?",
			userId, type);
	}

	/** 그 회원의 알림 전체 수 (종류 무관) */
	private long count(long userId) {
		return jdbcTemplate.queryForObject("select count(*) from tb_notification where user_id = ?", Long.class,
			userId);
	}

	private MvcTestResult post(String bearer, String uri, String json) {
		return mvc.post().uri(uri).header(HttpHeaders.AUTHORIZATION, bearer).contentType(MediaType.APPLICATION_JSON)
			.content(json).exchange();
	}
}
