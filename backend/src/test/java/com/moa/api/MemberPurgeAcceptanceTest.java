package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.jayway.jsonpath.JsonPath;
import com.moa.api.application.v1.user.MemberPurgeApplication;
import com.moa.api.service.user.MemberPurgeService;
import com.moa.common.security.Role;
import com.moa.common.security.recovery.RecoveryCookies;
import com.moa.common.security.recovery.RecoveryTicket;
import com.moa.support.AcceptanceTest;
import com.moa.support.DatabaseCleaner;
import com.moa.support.FakeOAuthProviderClient;
import com.moa.support.TestUsers;

import jakarta.servlet.http.Cookie;

/**
 * 유예가 끝난 회원 파기 (specs/018-member-withdrawal US3, US2-AC6, data-model "파기할 때", docs/privacy.md).
 * 정리기(MemberPurgeCleaner)가 부르는 MemberPurgeApplication.purgeDue()를 바로 부른다.
 * 시각은 DB의 now()가 아니라 서버(JVM)의 지금으로 넣는다. 실패·로그·이벤트는 MemberPurgeFailureAcceptanceTest가 본다.
 */
class MemberPurgeAcceptanceTest extends AcceptanceTest {

	private static final long SYSTEM_ID = 1L;
	private static final long DUE_ID = 10L;
	private static final long GRACE_ID = 11L;
	private static final long ACTIVE_ID = 12L;
	private static final long DELETED_DUE_ID = 13L;
	private static final long DELETED_RECENT_ID = 14L;
	private static final long DELETED_ADMIN_ID = 15L;
	private static final String PASSWORD = "member1234!";
	private static final String OAUTH = "/api/v1/auth/oauth";

	/** docs/privacy.md 보관 기간 표에서 회원 파기 때 지우는 행 */
	private static final List<String> OWNED_TABLES = List.of("tb_user_profile", "tb_user_profile_play_time",
		"tb_user_game_profile", "tb_user_social_account", "tb_user_terms_agreement", "tb_notification",
		"tb_push_subscription", "tb_notice_read", "tb_refresh_token");

	@Autowired
	private MemberPurgeApplication memberPurgeApplication;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DatabaseCleaner databaseCleaner;

	@Autowired
	private RecoveryCookies recoveryCookies;

	@Autowired
	private FakeOAuthProviderClient provider;

	@Autowired
	private MemberPurgeService memberPurgeService;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@BeforeEach
	void setUp() {
		databaseCleaner.clean();
		testUsers.create(SYSTEM_ID, "system", null, Role.USER);
	}

	@AfterEach
	void resetProvider() {
		provider.reset();
	}

	@Test
	@DisplayName("US3-AC1·AC2·AC3·AC5·AC9 탈퇴한 지 30일이 지난 회원: 개인정보 열이 덮이고 딸린 행이 모두 지워지며 파기 이력이 1건 남는다. 접속 이력은 남는다")
	void purgesWithdrawnMember() {
		member(DUE_ID, "due_member");
		Map<String, Object> before = userRow(DUE_ID);
		withdrawnAt(DUE_ID, LocalDateTime.now().minusDays(31));

		assertThat(memberPurgeApplication.purgeDue()).isOne();

		Map<String, Object> after = userRow(DUE_ID);
		assertThat(after.get("login_id")).isEqualTo("withdrawn_" + DUE_ID);
		assertThat(after.get("password")).isNull();
		assertThat(after.get("name")).isEqualTo("탈퇴한 회원");
		assertThat(after.get("purged_at")).isNotNull();
		assertThat(after.get("deleted_at")).isNotNull();
		assertThat(after.get("deleted_by")).isEqualTo(SYSTEM_ID);
		assertThat(after.get("withdrawn_at")).as("탈퇴한 시각은 개인정보가 아니라 남는다").isNotNull();
		// 모든 문자열 칸에 원래 값(아이디, 이름, 이메일, 비밀번호 해시)이 남지 않는다 (data-model "파기할 때")
		List<String> originals = List.of("due_member", "due_member@example.com", (String)before.get("password"));
		after.values().stream().filter(String.class::isInstance).map(String.class::cast)
			.forEach(value -> assertThat(originals).noneMatch(value::contains));
		for (String table : OWNED_TABLES) {
			assertThat(rowCount(table, DUE_ID)).as(table).isZero();
		}
		assertThat(rowCount("tb_login_history", DUE_ID)).isOne();
		List<Map<String, Object>> logs = jdbcTemplate.queryForList("select * from tb_purge_log");
		assertThat(logs).singleElement().satisfies(log -> {
			assertThat(log.get("target")).isEqualTo("MEMBER");
			assertThat(log.get("purge_method")).isEqualTo("COLUMN_ERASE");
			assertThat(log.get("purged_count")).isEqualTo(1);
			assertThat(log.get("actor_type")).isEqualTo("SCHEDULER");
			assertThat(log.get("actor_user_id")).isNull();
			assertThat(log.get("target_user_id")).isEqualTo(DUE_ID);
		});
	}

	@Test
	@DisplayName("US3-AC4·AC11·AC15 29일째·활동 회원, 지운 지 30일 안 된 회원, 지운 관리자, 시스템 계정은 그대로이고 "
		+ "지운 지 30일이 지난 회원(이 기능 전에 지움)은 파기된다")
	void onlyDueMembersArePurged() {
		member(DUE_ID, "due_member");
		member(GRACE_ID, "grace_member");
		member(ACTIVE_ID, "active_member");
		member(DELETED_DUE_ID, "deleted_due");
		member(DELETED_RECENT_ID, "deleted_recent");
		testUsers.create(DELETED_ADMIN_ID, "deleted_admin", PASSWORD, Role.ADMIN);
		LocalDateTime now = LocalDateTime.now();
		withdrawnAt(DUE_ID, now.minusDays(31));
		withdrawnAt(GRACE_ID, now.minusDays(29));
		deletedAt(DELETED_DUE_ID, now.minusDays(40));
		deletedAt(DELETED_RECENT_ID, now.minusDays(29));
		deletedAt(DELETED_ADMIN_ID, now.minusDays(40));
		deletedAt(SYSTEM_ID, now.minusDays(40));
		List<Long> untouched = List.of(GRACE_ID, ACTIVE_ID, DELETED_RECENT_ID, DELETED_ADMIN_ID, SYSTEM_ID);
		Map<Long, String> snapshots = snapshots(untouched);
		Object adminDeletedAt = userRow(DELETED_DUE_ID).get("deleted_at");

		assertThat(memberPurgeApplication.purgeDue()).isEqualTo(2);

		assertThat(snapshots(untouched)).isEqualTo(snapshots);
		assertThat(
			jdbcTemplate.queryForList("select id from tb_user where purged_at is not null order by id", Long.class))
			.containsExactly(DUE_ID, DELETED_DUE_ID);
		Map<String, Object> deleted = userRow(DELETED_DUE_ID);
		assertThat(deleted.get("login_id")).isEqualTo("withdrawn_" + DELETED_DUE_ID);
		assertThat(deleted.get("deleted_at")).as("관리자가 지운 기록은 그대로").isEqualTo(adminDeletedAt);
		for (String table : OWNED_TABLES) {
			assertThat(rowCount(table, DELETED_DUE_ID)).as(table).isZero();
		}
		assertThat(jdbcTemplate.queryForList("select target_user_id from tb_purge_log order by target_user_id",
			Long.class)).containsExactly(DUE_ID, DELETED_DUE_ID);
	}

	@Test
	@DisplayName("US3-AC8 두 번 돌려도 두 번째는 0명이고 아무것도 바뀌지 않으며 파기 이력도 늘지 않는다")
	void secondRunChangesNothing() {
		member(DUE_ID, "due_member");
		withdrawnAt(DUE_ID, LocalDateTime.now().minusDays(31));
		assertThat(memberPurgeApplication.purgeDue()).isOne();
		String purged = snapshots(List.of(DUE_ID)).get(DUE_ID);

		assertThat(memberPurgeApplication.purgeDue()).isZero();

		assertThat(snapshots(List.of(DUE_ID)).get(DUE_ID)).isEqualTo(purged);
		assertThat(count("select count(*) from tb_purge_log")).isOne();
	}

	@Test
	@DisplayName("US2-AC6 복구한 회원은 처음 탈퇴한 때부터 30일이 지나도 파기되지 않는다")
	void recoveredMemberIsNotPurged() {
		member(DUE_ID, "due_member");
		LocalDateTime withdrawn = LocalDateTime.now().minusDays(29).withNano(0);
		withdrawnAt(DUE_ID, withdrawn);
		assertThat(mvc.post().uri("/api/v1/auth/recovery").cookie(ticket(DUE_ID, withdrawn)).exchange())
			.hasStatus(200);
		jdbcTemplate.update("update tb_user set created_at = ? where id = ?", LocalDateTime.now().minusDays(60),
			DUE_ID);

		assertThat(memberPurgeApplication.purgeDue()).isZero();

		assertThat(userRow(DUE_ID).get("purged_at")).isNull();
		assertThat(rowCount("tb_user_profile", DUE_ID)).isOne();
	}

	@Test
	@DisplayName("US3-AC10 파기된 회원의 닉네임과 태그는 다른 회원이 다시 쓸 수 있다")
	void nicknameTagIsReleased() {
		member(DUE_ID, "due_member");
		withdrawnAt(DUE_ID, LocalDateTime.now().minusDays(31));
		memberPurgeApplication.purgeDue();
		testUsers.create(ACTIVE_ID, "active_member", PASSWORD, Role.USER);

		jdbcTemplate.update("insert into tb_user_profile (user_id, nickname_key, nickname_tag, created_at, updated_at) "
			+ "values (?, 'due_member', '0421', now(6), now(6))", ACTIVE_ID);

		assertThat(rowCount("tb_user_profile", ACTIVE_ID)).isOne();
	}

	@Test
	@DisplayName("US3-AC12·AC13 소셜 회원은 파기된 뒤 같은 소셜 계정으로 새로 가입하고, 같은 이메일의 다른 소셜 계정·같은 로그인 아이디도 막히지 않는다")
	void socialMemberCanSignUpAgain() {
		String admin = superAdmin();
		long socialId = socialMember("google-sub-0001", "kim@example.com");
		String loginId = (String)userRow(socialId).get("login_id");
		withdrawnAt(socialId, LocalDateTime.now().minusDays(31));
		assertThat(memberPurgeApplication.purgeDue()).isOne();

		assertThat(location(socialCallback("code-again", "google-sub-0001", "kim@example.com")))
			.isEqualTo("/sign-up/nickname");
		assertThat(location(socialCallback("code-other", "google-sub-9999", "kim@example.com")))
			.isEqualTo("/sign-up/nickname");
		MvcTestResult created = mvc.post().uri("/api/v1/admin/users").header(HttpHeaders.AUTHORIZATION, admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"loginId\":\"" + loginId + "\",\"name\":\"새 계정\",\"role\":\"ADMIN\"}").exchange();
		assertThat(created).hasStatus(201);
	}

	@Test
	@DisplayName("US3-AC16 관리자가 지운 소셜 회원이 파기되면 같은 소셜 계정으로 새 회원으로 가입할 수 있다")
	void adminDeletedSocialMemberCanSignUpAgain() {
		long socialId = socialMember("google-sub-0002", "lee@example.com");
		deletedAt(socialId, LocalDateTime.now().minusDays(31));
		assertThat(location(socialCallback("code-before", "google-sub-0002", "lee@example.com")))
			.as("파기 전에는 지금처럼 연결이 남아 있어 막힌다").isEqualTo("/sign-in?oauthError=emailInUse");

		assertThat(memberPurgeApplication.purgeDue()).isOne();

		assertThat(location(socialCallback("code-after", "google-sub-0002", "lee@example.com")))
			.isEqualTo("/sign-up/nickname");
	}

	@Test
	@DisplayName("Edge 지운 회원의 아이디를 새 계정이 쓰는 중이면 지운 회원의 행만 파기하고 새 계정은 그대로다")
	void reusedLoginIdIsUntouched() {
		member(DELETED_DUE_ID, "reused");
		deletedAt(DELETED_DUE_ID, LocalDateTime.now().minusDays(31));
		testUsers.create(ACTIVE_ID, "reused", PASSWORD, Role.USER);
		String active = snapshots(List.of(ACTIVE_ID)).get(ACTIVE_ID);

		assertThat(memberPurgeApplication.purgeDue()).isOne();

		assertThat(snapshots(List.of(ACTIVE_ID)).get(ACTIVE_ID)).isEqualTo(active);
		assertThat(userRow(DELETED_DUE_ID).get("login_id")).isEqualTo("withdrawn_" + DELETED_DUE_ID);
	}

	@Test
	@DisplayName("Edge 여러 서버에서 동시에 돌아도 회원마다 한 번만 파기되고 파기 이력도 하나다")
	void concurrentPurges() throws Exception {
		member(DUE_ID, "due_member");
		withdrawnAt(DUE_ID, LocalDateTime.now().minusDays(31));

		List<Integer> purged = concurrently(memberPurgeApplication::purgeDue, memberPurgeApplication::purgeDue);

		assertThat(purged.stream().mapToInt(Integer::intValue).sum()).isOne();
		assertThat(count("select count(*) from tb_purge_log")).isOne();
	}

	@Test
	@DisplayName("Edge 복구와 파기가 겹치면 하나만 된다: 파기 대상을 읽은 뒤 복구됐으면 행을 잠그고 다시 보아 파기하지 않는다")
	void recoveredAfterListingIsNotPurged() {
		member(DUE_ID, "due_member");
		withdrawnAt(DUE_ID, LocalDateTime.now().minusDays(31));
		LocalDateTime now = LocalDateTime.now();
		assertThat(memberPurgeService.findDueIds(now)).containsExactly(DUE_ID);

		jdbcTemplate.update("update tb_user set withdrawn_at = null where id = ?", DUE_ID);

		Boolean erased = transactionTemplate.execute(status -> memberPurgeService.eraseIfDue(DUE_ID, now));
		assertThat(erased).isFalse();
		assertThat(userRow(DUE_ID).get("purged_at")).isNull();
		assertThat(rowCount("tb_user_profile", DUE_ID)).isOne();
	}

	// ---------- 도우미 ----------

	/** 비밀번호 회원과 docs/privacy.md 표의 딸린 행 모두, 접속 이력 하나 */
	private void member(long id, String loginId) {
		testUsers.create(id, loginId, PASSWORD, Role.USER);
		jdbcTemplate.update("insert into tb_user_profile (user_id, nickname_key, nickname_tag, created_at, updated_at) "
			+ "values (?, ?, '0421', now(6), now(6))", id, loginId);
		jdbcTemplate.update("insert into tb_user_profile_play_time (user_id, play_time_code) values (?, 'DAWN')", id);
		jdbcTemplate.update("insert into tb_user_game_profile (user_id, game_code, game_account_id, created_at, "
			+ "updated_at, created_by, updated_by) values (?, 'LOL', ?, now(6), now(6), 1, 1)", id, loginId + "#kr");
		jdbcTemplate.update("insert into tb_user_social_account (user_id, provider, subject, email, display_name, "
			+ "created_at) values (?, 'GOOGLE', ?, ?, 'Name', now(6))", id, "sub-" + id, loginId + "@example.com");
		jdbcTemplate.update("insert into tb_user_terms_agreement (user_id, terms_type, terms_version, agreed_at) "
			+ "values (?, 'SERVICE', 'draft', now(6))", id);
		jdbcTemplate.update("insert into tb_notification (user_id, type, title, link, created_at) "
			+ "values (?, 'SYSTEM', '알림', '/', now(6))", id);
		jdbcTemplate.update("insert into tb_push_subscription (user_id, endpoint, p256dh, auth, created_at) "
			+ "values (?, ?, 'p256dh', 'auth', now(6))", id, "https://push.test/" + id);
		jdbcTemplate.update("insert into tb_notice_read (user_id, last_seen_at) values (?, now(6))", id);
		jdbcTemplate.update("insert into tb_refresh_token (user_id, family_id, token_hash, expires_at, created_at, "
			+ "updated_at) values (?, uuid(), sha2(uuid(), 256), now(6) + interval 14 day, now(6), now(6))", id);
		jdbcTemplate.update("insert into tb_login_history (user_id, login_id, success, device_type, os, browser, "
			+ "created_at, updated_at) values (?, ?, true, 'DESKTOP', 'Windows', 'Chrome', now(6), now(6))", id,
			loginId);
	}

	/** 회원 행과 딸린 행 모두를 한 문자열로 (바뀌지 않았는지 비교) */
	private Map<Long, String> snapshots(List<Long> ids) {
		Map<Long, String> result = new java.util.TreeMap<>();
		for (Long id : ids) {
			StringBuilder snapshot = new StringBuilder(userRow(id).toString());
			for (String table : OWNED_TABLES) {
				snapshot.append('|').append(table).append('=')
					.append(jdbcTemplate.queryForList("select * from " + table + " where user_id = ? order by 1", id));
			}
			result.put(id, snapshot.toString());
		}
		return result;
	}

	private Map<String, Object> userRow(long id) {
		return jdbcTemplate.queryForMap("select * from tb_user where id = ?", id);
	}

	private void withdrawnAt(long id, LocalDateTime at) {
		jdbcTemplate.update("update tb_user set withdrawn_at = ? where id = ?", at, id);
	}

	private void deletedAt(long id, LocalDateTime at) {
		jdbcTemplate.update("update tb_user set deleted_at = ?, deleted_by = 1 where id = ?", at, id);
	}

	private int rowCount(String table, long userId) {
		return count("select count(*) from " + table + " where user_id = ?", userId);
	}

	private int count(String sql, Object... args) {
		return jdbcTemplate.queryForObject(sql, Integer.class, args);
	}

	private Cookie ticket(long userId, LocalDateTime withdrawnAt) {
		MockHttpServletResponse response = new MockHttpServletResponse();
		recoveryCookies.write(response, new RecoveryTicket(userId, withdrawnAt));
		return response.getCookie(RecoveryCookies.COOKIE);
	}

	private long socialMember(String subject, String email) {
		Cookie signup = socialCallback("code-" + subject, subject, email).getResponse().getCookie("oauth_signup");
		MvcTestResult signedUp = mvc.post().uri(OAUTH + "/sign-up").cookie(signup)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nickname\":\"소셜" + subject.substring(subject.length() - 4) + "\",\"agreements\":["
				+ "{\"type\":\"SERVICE\",\"version\":\"draft\"},{\"type\":\"PRIVACY\",\"version\":\"draft\"}]}")
			.exchange();
		assertThat(signedUp).hasStatus(200);
		String token = JsonPath.read(body(signedUp), "$.accessToken");
		Number id = JsonPath.read(body(mvc.get().uri("/api/v1/users/me")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).exchange()), "$.id");
		return id.longValue();
	}

	private MvcTestResult socialCallback(String code, String subject, String email) {
		provider.register(code, subject, email, true, "Provider Name");
		MvcTestResult authorize = mvc.get().uri(OAUTH + "/google/authorize").exchange();
		String state = UriComponentsBuilder.fromUriString(location(authorize)).build().getQueryParams()
			.getFirst("state");
		MvcTestResult callback = mvc.get().uri(OAUTH + "/google/callback?code={code}&state={state}", code, state)
			.cookie(authorize.getResponse().getCookie("oauth_state")).exchange();
		assertThat(callback).hasStatus(302);
		return callback;
	}

	private String location(MvcTestResult result) {
		return result.getResponse().getHeader(HttpHeaders.LOCATION);
	}

	private String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString();
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@SafeVarargs
	private <T> List<T> concurrently(Callable<T>... tasks) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(tasks.length);
		try {
			CountDownLatch start = new CountDownLatch(1);
			List<Future<T>> futures = new ArrayList<>();
			for (Callable<T> task : tasks) {
				futures.add(executor.submit(() -> {
					start.await();
					return task.call();
				}));
			}
			start.countDown();
			List<T> results = new ArrayList<>();
			for (Future<T> future : futures) {
				results.add(future.get());
			}
			return results;
		} finally {
			executor.shutdown();
		}
	}
}
