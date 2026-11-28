package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;

import com.jayway.jsonpath.JsonPath;
import com.moa.api.application.v1.user.MemberPurgeApplication;
import com.moa.api.dto.user.MemberPurgedEvent;
import com.moa.api.dto.user.MemberRestoredEvent;
import com.moa.api.dto.user.MemberWithdrawnEvent;
import com.moa.common.purgelog.PurgeLog;
import com.moa.common.purgelog.PurgeLogRepository;
import com.moa.common.security.Role;
import com.moa.common.security.recovery.RecoveryCookies;
import com.moa.common.security.token.RefreshTokenEraser;
import com.moa.support.AcceptanceTest;
import com.moa.support.DatabaseCleaner;
import com.moa.support.FakeOAuthProviderClient;
import com.moa.support.TestUsers;

import jakarta.servlet.http.Cookie;

/**
 * 파기가 실패할 때, 로그, 다른 기능이 붙는 이벤트 (specs/018-member-withdrawal US3-AC6·AC7·AC14, US4-AC3~AC6).
 * 스파이와 시험용 리스너를 쓰므로 컨텍스트가 따로 뜬다.
 */
@ExtendWith(OutputCaptureExtension.class)
@Import(MemberPurgeFailureAcceptanceTest.Listeners.class)
class MemberPurgeFailureAcceptanceTest extends AcceptanceTest {

	private static final long FIRST_ID = 10L;
	private static final long SECOND_ID = 11L;
	private static final String PASSWORD = "pw-secret-1234!";
	private static final String OAUTH = "/api/v1/auth/oauth";

	@MockitoSpyBean
	private PurgeLogRepository purgeLogRepository;

	@MockitoSpyBean
	private RefreshTokenEraser refreshTokenEraser;

	@Autowired
	private MemberPurgeApplication memberPurgeApplication;

	@Autowired
	private RecordingListener listener;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DatabaseCleaner databaseCleaner;

	@Autowired
	private FakeOAuthProviderClient provider;

	@BeforeEach
	void setUp() {
		databaseCleaner.clean();
		testUsers.create(1L, "system", null, Role.USER);
		listener.reset();
	}

	@AfterEach
	void tearDown() {
		provider.reset();
		listener.reset();
	}

	@Test
	@DisplayName("US3-AC6·US4-AC6 파기 이력을 남기지 못하면 그 회원의 것은 하나도 지워지지 않고 \"파기됨\"도 돌지 않으며, 다음 정리가 다시 파기한다")
	void purgeLogFailureRollsBack() {
		dueMember(FIRST_ID, "first_member");
		String before = snapshot(FIRST_ID);
		doThrow(new IllegalStateException("저장 실패")).when(purgeLogRepository).save(any(PurgeLog.class));

		assertThat(memberPurgeApplication.purgeDue()).isZero();

		assertThat(snapshot(FIRST_ID)).isEqualTo(before);
		assertThat(listener.purged).isEmpty();

		Mockito.reset(purgeLogRepository);
		assertThat(memberPurgeApplication.purgeDue()).isOne();
		assertThat(listener.purged).containsExactly(FIRST_ID);
	}

	@Test
	@DisplayName("US3-AC7 한 회원의 파기가 실패해도 같은 정리에서 다른 회원은 파기된다")
	void oneFailureDoesNotStopOthers() {
		dueMember(FIRST_ID, "first_member");
		dueMember(SECOND_ID, "second_member");
		doThrow(new IllegalStateException("지우기 실패")).when(refreshTokenEraser).deleteAllOf(eq(FIRST_ID));

		assertThat(memberPurgeApplication.purgeDue()).isOne();

		assertThat(purgedAt(FIRST_ID)).isNull();
		assertThat(count("select count(*) from tb_user_profile where user_id = ?", FIRST_ID)).isOne();
		assertThat(purgedAt(SECOND_ID)).isNotNull();
		assertThat(jdbcTemplate.queryForList("select target_user_id from tb_purge_log", Long.class))
			.containsExactly(SECOND_ID);
	}

	@Test
	@DisplayName("US3-AC14 소셜·비밀번호 회원이 탈퇴·복구·파기되는 동안의 로그에 이메일, 비밀번호, 토큰, 소셜 식별자가 없다")
	void logsHaveNoPersonalData(CapturedOutput output) {
		String email = "secret-mail@example.com";
		String subject = "google-sub-secret-777";
		MvcTestResult signedUp = socialSignUp(subject, email);
		String socialToken = JsonPath.read(body(signedUp), "$.accessToken");
		String socialRefresh = signedUp.getResponse().getCookie("refresh_token").getValue();
		long socialId = idOf(socialToken);
		testUsers.create(SECOND_ID, "password_member", PASSWORD, Role.USER);

		assertThat(withdraw("Bearer " + socialToken, nameOf(socialId))).hasStatus(200);
		Cookie recovery = socialCallback("code-back", subject, email).getResponse().getCookie(RecoveryCookies.COOKIE);
		MvcTestResult recovered = mvc.post().uri("/api/v1/auth/recovery").cookie(recovery).exchange();
		assertThat(recovered).hasStatus(200);
		String recoveredToken = JsonPath.read(body(recovered), "$.accessToken");
		assertThat(withdraw("Bearer " + recoveredToken, nameOf(socialId))).hasStatus(200);
		assertThat(withdraw(bearer(SECOND_ID, "USER"), "password_member")).hasStatus(200);
		assertThat(login("password_member", PASSWORD)).hasStatus(401);
		assertThat(login("password_member", "wrong-" + PASSWORD)).hasStatus(401);
		jdbcTemplate.update("update tb_user set withdrawn_at = ? where id in (?, ?)", LocalDateTime.now().minusDays(31),
			socialId, SECOND_ID);
		assertThat(memberPurgeApplication.purgeDue()).isEqualTo(2);

		String logs = output.getAll();
		assertThat(logs).contains("회원 탈퇴: userId=" + socialId);
		for (String secret : List.of(email, subject, PASSWORD, socialToken, socialRefresh, recoveredToken,
			recovery.getValue())) {
			assertThat(logs).doesNotContain(secret);
		}
	}

	@Test
	@DisplayName("US4-AC3·AC5·AC6 \"탈퇴됨\"·\"복구됨\"·\"파기됨\"에 붙인 처리가 저장된 뒤 그 회원의 내부 ID로 돈다")
	void listenersRunAfterCommit() {
		testUsers.create(FIRST_ID, "first_member", PASSWORD, Role.USER);

		assertThat(withdraw(bearer(FIRST_ID, "USER"), "first_member")).hasStatus(200);
		assertThat(listener.withdrawn).containsExactly(FIRST_ID);
		assertThat(listener.withdrawnSeenInDb).containsExactly(true);

		Cookie recovery = login("first_member", PASSWORD).getResponse().getCookie(RecoveryCookies.COOKIE);
		assertThat(mvc.post().uri("/api/v1/auth/recovery").cookie(recovery).exchange()).hasStatus(200);
		assertThat(listener.restored).containsExactly(FIRST_ID);

		jdbcTemplate.update("update tb_user set withdrawn_at = ? where id = ?", LocalDateTime.now().minusDays(31),
			FIRST_ID);
		assertThat(memberPurgeApplication.purgeDue()).isOne();
		assertThat(listener.purged).containsExactly(FIRST_ID);
	}

	@Test
	@DisplayName("US4-AC4 세 이벤트에 붙은 처리가 실패해도 탈퇴·복구·파기는 그대로 끝난다")
	void failingListenersDoNotBreakFlows() {
		listener.fail = true;
		testUsers.create(FIRST_ID, "first_member", PASSWORD, Role.USER);

		assertThat(withdraw(bearer(FIRST_ID, "USER"), "first_member")).hasStatus(200);
		assertThat(withdrawnAt(FIRST_ID)).isNotNull();

		Cookie recovery = login("first_member", PASSWORD).getResponse().getCookie(RecoveryCookies.COOKIE);
		assertThat(mvc.post().uri("/api/v1/auth/recovery").cookie(recovery).exchange()).hasStatus(200);
		assertThat(withdrawnAt(FIRST_ID)).isNull();

		jdbcTemplate.update("update tb_user set withdrawn_at = ? where id = ?", LocalDateTime.now().minusDays(31),
			FIRST_ID);
		assertThat(memberPurgeApplication.purgeDue()).isOne();
		assertThat(purgedAt(FIRST_ID)).isNotNull();
		assertThat(listener.withdrawn).containsExactly(FIRST_ID);
		assertThat(listener.restored).containsExactly(FIRST_ID);
		assertThat(listener.purged).containsExactly(FIRST_ID);
	}

	// ---------- 시험용 리스너 (다른 기능이 붙는 모양, MemberWithdrawnEvent Javadoc) ----------

	@TestConfiguration
	static class Listeners {

		@Bean
		RecordingListener recordingListener(JdbcTemplate jdbcTemplate) {
			return new RecordingListener(jdbcTemplate);
		}
	}

	static class RecordingListener {

		final List<Long> withdrawn = new CopyOnWriteArrayList<>();
		final List<Boolean> withdrawnSeenInDb = new CopyOnWriteArrayList<>();
		final List<Long> restored = new CopyOnWriteArrayList<>();
		final List<Long> purged = new CopyOnWriteArrayList<>();
		volatile boolean fail;
		private final JdbcTemplate jdbcTemplate;

		RecordingListener(JdbcTemplate jdbcTemplate) {
			this.jdbcTemplate = jdbcTemplate;
		}

		@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
		public void onWithdrawn(MemberWithdrawnEvent event) {
			withdrawn.add(event.userId());
			// 커밋된 뒤라 다른 연결에서도 탈퇴가 보인다
			withdrawnSeenInDb.add(jdbcTemplate.queryForObject(
				"select withdrawn_at is not null from tb_user where id = ?", Boolean.class, event.userId()));
			failIfAsked();
		}

		@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
		public void onRestored(MemberRestoredEvent event) {
			restored.add(event.userId());
			failIfAsked();
		}

		@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
		public void onPurged(MemberPurgedEvent event) {
			purged.add(event.userId());
			failIfAsked();
		}

		void reset() {
			withdrawn.clear();
			withdrawnSeenInDb.clear();
			restored.clear();
			purged.clear();
			fail = false;
		}

		private void failIfAsked() {
			if (fail) {
				throw new IllegalStateException("붙은 처리 실패");
			}
		}
	}

	// ---------- 도우미 ----------

	/** 탈퇴한 지 31일이 지난 회원과 딸린 행 */
	private void dueMember(long id, String loginId) {
		testUsers.create(id, loginId, PASSWORD, Role.USER);
		jdbcTemplate.update("insert into tb_user_profile (user_id, nickname_key, nickname_tag, created_at, updated_at) "
			+ "values (?, ?, '0421', now(6), now(6))", id, loginId);
		jdbcTemplate.update("insert into tb_user_social_account (user_id, provider, subject, email, display_name, "
			+ "created_at) values (?, 'GOOGLE', ?, ?, 'Name', now(6))", id, "sub-" + id, loginId + "@example.com");
		jdbcTemplate.update("insert into tb_notice_read (user_id, last_seen_at) values (?, now(6))", id);
		jdbcTemplate.update("update tb_user set withdrawn_at = ? where id = ?", LocalDateTime.now().minusDays(31), id);
	}

	private String snapshot(long id) {
		StringBuilder snapshot = new StringBuilder(
			jdbcTemplate.queryForMap("select * from tb_user where id = ?", id).toString());
		for (String table : List.of("tb_user_profile", "tb_user_social_account", "tb_notice_read")) {
			snapshot.append('|').append(jdbcTemplate.queryForList("select * from " + table + " where user_id = ?", id));
		}
		return snapshot + "|logs=" + count("select count(*) from tb_purge_log");
	}

	private MvcTestResult withdraw(String bearer, String nickname) {
		return mvc.post().uri("/api/v1/users/me/withdrawal").header(HttpHeaders.AUTHORIZATION, bearer)
			.contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"" + nickname + "\"}").exchange();
	}

	private MvcTestResult login(String loginId, String password) {
		return mvc.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"loginId\":\"" + loginId + "\",\"password\":\"" + password + "\"}").exchange();
	}

	private MvcTestResult socialSignUp(String subject, String email) {
		Cookie signup = socialCallback("code-" + subject, subject, email).getResponse().getCookie("oauth_signup");
		MvcTestResult signedUp = mvc.post().uri(OAUTH + "/sign-up").cookie(signup)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nickname\":\"소셜회원\",\"agreements\":[{\"type\":\"SERVICE\",\"version\":\"draft\"},"
				+ "{\"type\":\"PRIVACY\",\"version\":\"draft\"}]}")
			.exchange();
		assertThat(signedUp).hasStatus(200);
		return signedUp;
	}

	private MvcTestResult socialCallback(String code, String subject, String email) {
		provider.register(code, subject, email, true, "Provider Name");
		MvcTestResult authorize = mvc.get().uri(OAUTH + "/google/authorize").exchange();
		String state = UriComponentsBuilder.fromUriString(authorize.getResponse().getHeader(HttpHeaders.LOCATION))
			.build().getQueryParams().getFirst("state");
		MvcTestResult callback = mvc.get().uri(OAUTH + "/google/callback?code={code}&state={state}", code, state)
			.cookie(authorize.getResponse().getCookie("oauth_state")).exchange();
		assertThat(callback).hasStatus(302);
		return callback;
	}

	private long idOf(String accessToken) {
		Number id = JsonPath.read(body(mvc.get().uri("/api/v1/users/me")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).exchange()), "$.id");
		return id.longValue();
	}

	private String nameOf(long id) {
		return jdbcTemplate.queryForObject("select name from tb_user where id = ?", String.class, id);
	}

	private LocalDateTime withdrawnAt(long id) {
		return jdbcTemplate.queryForObject("select withdrawn_at from tb_user where id = ?", LocalDateTime.class, id);
	}

	private LocalDateTime purgedAt(long id) {
		return jdbcTemplate.queryForObject("select purged_at from tb_user where id = ?", LocalDateTime.class, id);
	}

	private int count(String sql, Object... args) {
		return jdbcTemplate.queryForObject(sql, Integer.class, args);
	}

	private String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString();
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
