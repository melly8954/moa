package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;
import com.moa.api.scheduler.NotificationCleaner;
import com.moa.common.audit.SystemActor;
import com.moa.common.helper.loginhistory.LoginHistoryCleaner;
import com.moa.common.helper.richtext.RichTextImageCleaner;
import com.moa.common.purgelog.PurgeLogCleaner;
import com.moa.common.security.Role;
import com.moa.common.security.token.RefreshTokenCleaner;
import com.moa.support.AcceptanceTest;
import com.moa.support.MenuSeed;
import com.moa.support.TestUsers;

/**
 * 개인정보 파기 기록. 스펙 specs/013-purge-log의 수용 시나리오다
 */
@TestPropertySource(properties = "app.rich-text.image-dir=build/tmp/rich-text-images")
class PurgeLogAcceptanceTest extends AcceptanceTest {

	private static final long OPERATOR_ID = 10L;
	private static final String PURGE_LOGS = "/api/v1/admin/purge-logs";
	private static final byte[] PNG = {(byte)0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13, 'I', 'H', 'D',
		'R'};

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MenuSeed menuSeed;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private LoginHistoryCleaner loginHistoryCleaner;

	@Autowired
	private NotificationCleaner notificationCleaner;

	@Autowired
	private RefreshTokenCleaner refreshTokenCleaner;

	@Autowired
	private RichTextImageCleaner richTextImageCleaner;

	@Autowired
	private PurgeLogCleaner purgeLogCleaner;

	/** 본문 이미지 폴더 설정 때문에 이 클래스는 새 컨텍스트로 뜬다. 첫 테스트에는 마이그레이션 시드의 계정이 남아 있어 먼저 지운다 */
	@BeforeEach
	void setUp() {
		menuSeed.restore();
		jdbcTemplate.update("delete from tb_user where id in (1, 2)");
		testUsers.create(1L, "system", null, Role.USER);
		testUsers.create(2L, "admin", "admin1234!", Role.ADMIN);
	}

	@Test
	@DisplayName("US1-AC1 보관 기간이 지난 접속 이력 3건을 지우면 접속 이력·3건·행 삭제·정리기 파기 기록 1건이 남는다")
	void loginHistoryCleanupLeavesPurgeLog() {
		for (int i = 0; i < 3; i++) {
			loginHistory("old-" + i, LocalDateTime.now().minusDays(400));
		}

		SystemActor.run(loginHistoryCleaner::deleteExpired);

		List<Map<String, Object>> logs = purgeLogs();
		assertThat(logs).hasSize(1);
		assertThat(logs.get(0)).containsEntry("target", "LOGIN_HISTORY")
			.containsEntry("purged_count", 3)
			.containsEntry("purge_method", "ROW_DELETE")
			.containsEntry("actor_type", "SCHEDULER");
		assertThat(jdbcTemplate.queryForObject("select created_at from tb_purge_log", LocalDateTime.class))
			.isAfter(LocalDateTime.now().minusMinutes(1));
	}

	@Test
	@DisplayName("US1-AC2 보관 기간이 지난 알림 2건을 지우면 알림·2건 파기 기록 1건이 남는다")
	void notificationCleanupLeavesPurgeLog() {
		notification("old-1", LocalDateTime.now().minusDays(91));
		notification("old-2", LocalDateTime.now().minusDays(91));

		SystemActor.run(notificationCleaner::deleteExpired);

		assertThat(purgeLogs()).singleElement().satisfies(log -> assertThat(log)
			.containsEntry("target", "NOTIFICATION").containsEntry("purged_count", 2));
	}

	@Test
	@DisplayName("US1-AC3 만료된 지 하루가 지난 리프레시 토큰 2건을 지우면 리프레시 토큰·2건 파기 기록 1건이 남는다")
	void refreshTokenCleanupLeavesPurgeLog() {
		refreshToken("a".repeat(64), LocalDateTime.now().minusDays(2));
		refreshToken("b".repeat(64), LocalDateTime.now().minusDays(2));

		SystemActor.run(refreshTokenCleaner::deleteExpired);

		assertThat(purgeLogs()).singleElement().satisfies(log -> assertThat(log)
			.containsEntry("target", "REFRESH_TOKEN").containsEntry("purged_count", 2));
	}

	@Test
	@DisplayName("US1-AC4 보관 시간이 지난 본문 이미지 1개를 지우면 본문 이미지·1건 파기 기록 1건이 남는다")
	void richTextImageCleanupLeavesPurgeLog() {
		MvcTestResult uploaded = mvc.post().uri("/api/v1/rich-text/images").multipart()
			.file(new MockMultipartFile("file", "a.png", "application/octet-stream", PNG))
			.header(HttpHeaders.AUTHORIZATION, bearer(3L, "USER"))
			.exchange();
		assertThat(uploaded).hasStatus(201);
		jdbcTemplate.update("update tb_rich_text_image set updated_at = now(6) - interval 2 day");

		SystemActor.run(richTextImageCleaner::deleteExpired);

		assertThat(purgeLogs()).singleElement().satisfies(log -> assertThat(log)
			.containsEntry("target", "RICH_TEXT_IMAGE").containsEntry("purged_count", 1));
	}

	@Test
	@DisplayName("US1-AC5 보관 기간이 지난 접속 이력이 없으면 정리가 돌아도 파기 기록이 남지 않는다")
	void noPurgeLogWhenNothingDeleted() {
		loginHistory("recent", LocalDateTime.now().minusDays(1));

		SystemActor.run(loginHistoryCleaner::deleteExpired);

		assertThat(purgeLogs()).isEmpty();
	}

	@Test
	@DisplayName("US1-AC6 파기 기록에는 일시·대상 종류·건수·방법·처리자만 있고 지운 이력의 아이디·IP·브라우저 값은 없다")
	void purgeLogHasNoPersonalData() {
		jdbcTemplate.update("insert into tb_login_history (user_id, login_id, success, failure_code, ip, user_agent, "
			+ "device_type, os, browser, created_at, updated_at) values (10, 'secret-kim', true, null, '203.0.113.10', "
			+ "'Chrome/130.0', 'PC', 'Windows', 'Chrome', ?, ?)", LocalDateTime.now().minusDays(400),
			LocalDateTime.now().minusDays(400));

		SystemActor.run(loginHistoryCleaner::deleteExpired);

		Map<String, Object> log = purgeLogs().get(0);
		assertThat(log.keySet()).containsExactlyInAnyOrder("id", "target", "purge_method", "purged_count",
			"actor_type", "actor_user_id", "target_user_id", "created_at", "updated_at");
		assertThat(log.get("actor_user_id")).isNull();
		assertThat(log.get("target_user_id")).isNull();
		assertThat(log.values()).noneMatch(value -> value != null && List.of("secret-kim", "203.0.113.10",
			"Chrome/130.0", "Chrome", "Windows").contains(value.toString()));
	}

	@Test
	@DisplayName("US2-AC1 기간과 종류로 찾으면 그 기간·종류의 파기 기록만 최신순으로 일시·종류·건수·방법·처리자와 함께 보인다")
	void adminFindsPurgeLogsByPeriodAndTarget() {
		purgeLog("LOGIN_HISTORY", 3, LocalDateTime.of(2026, 11, 10, 5, 0));
		purgeLog("LOGIN_HISTORY", 5, LocalDateTime.of(2026, 11, 12, 5, 0));
		purgeLog("NOTIFICATION", 2, LocalDateTime.of(2026, 11, 12, 5, 1));
		purgeLog("LOGIN_HISTORY", 7, LocalDateTime.of(2026, 11, 20, 5, 0));

		MvcTestResult result = asAdmin(
			mvc.get().uri(PURGE_LOGS + "?from=2026-11-10&to=2026-11-15&target=LOGIN_HISTORY"));

		assertThat(result).hasStatus(200);
		assertThat(result).bodyJson().extractingPath("$.totalElements").isEqualTo(2);
		assertThat(result).bodyJson().extractingPath("$.content[*].count").asArray().containsExactly(5, 3);
		assertThat(result).bodyJson().extractingPath("$.content[0].target").isEqualTo("LOGIN_HISTORY");
		assertThat(result).bodyJson().extractingPath("$.content[0].method").isEqualTo("ROW_DELETE");
		assertThat(result).bodyJson().extractingPath("$.content[0].actor").isEqualTo("SCHEDULER");
		assertThat(result).bodyJson().extractingPath("$.content[0].createdAt").asString()
			.startsWith("2026-11-12T05:00");
	}

	@Test
	@DisplayName("US2-AC2 파기 기록 조회 권한만 받은 운영자는 운영 아래 파기 기록을 보고 목록을 연다")
	void operatorWithReadPermissionSeesMenuAndList() {
		testUsers.create(OPERATOR_ID, "operator", "operator123!", Role.ADMIN);
		MvcTestResult role = asAdmin(mvc.post().uri("/api/v1/admin/roles").contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"PRIVACY\",\"name\":\"개인정보 담당\",\"permissions\":[\"PURGE_LOG_READ\"]}"));
		assertThat(role).hasStatus(201);
		Number roleId = JsonPath.read(body(role), "$.id");
		assertThat(asAdmin(mvc.put().uri("/api/v1/admin/users/" + OPERATOR_ID + "/roles")
			.contentType(MediaType.APPLICATION_JSON).content("{\"roleIds\":[" + roleId + "]}")))
			.hasStatus2xxSuccessful();

		MvcTestResult menus = asOperator(mvc.get().uri("/api/v1/menus/me"));
		assertThat(menus).bodyJson().extractingPath("$[*].name").asArray().containsExactly("운영");
		assertThat(menus).bodyJson().extractingPath("$[0].children[*].name").asArray().containsExactly("파기 기록");
		assertThat(asOperator(mvc.get().uri(PURGE_LOGS))).hasStatus(200);
	}

	@Test
	@DisplayName("US2-AC3 파기 기록 조회 권한이 없는 운영자가 목록을 요청하면 권한 없음으로 막힌다")
	void operatorWithoutPermissionIsForbidden() {
		testUsers.create(OPERATOR_ID, "operator", "operator123!", Role.ADMIN);

		assertThat(asOperator(mvc.get().uri(PURGE_LOGS))).hasStatus(403)
			.bodyJson().extractingPath("$.code").isEqualTo("A005");
	}

	@Test
	@DisplayName("US3-AC1 366일 전 파기 기록만 지워지고, 이 정리에 대한 파기 기록은 새로 남지 않는다")
	void purgeLogsOlderThanOneYearAreDeletedWithoutNewLog() {
		purgeLog("LOGIN_HISTORY", 1, LocalDateTime.now().minusDays(366));
		purgeLog("NOTIFICATION", 1, LocalDateTime.now().minusDays(364));

		SystemActor.run(purgeLogCleaner::deleteExpired);

		assertThat(purgeLogs()).singleElement().satisfies(log -> assertThat(log)
			.containsEntry("target", "NOTIFICATION"));
	}

	private List<Map<String, Object>> purgeLogs() {
		return jdbcTemplate.queryForList("select * from tb_purge_log order by id");
	}

	private void purgeLog(String target, int count, LocalDateTime createdAt) {
		jdbcTemplate.update("insert into tb_purge_log (target, purge_method, purged_count, actor_type, created_at, "
			+ "updated_at) values (?, 'ROW_DELETE', ?, 'SCHEDULER', ?, ?)", target, count, createdAt, createdAt);
	}

	private void loginHistory(String loginId, LocalDateTime createdAt) {
		jdbcTemplate.update("insert into tb_login_history (user_id, login_id, success, failure_code, device_type, os, "
			+ "browser, created_at, updated_at) values (null, ?, false, 'A006', 'PC', 'Windows', 'Chrome', ?, ?)",
			loginId, createdAt, createdAt);
	}

	private void notification(String title, LocalDateTime createdAt) {
		jdbcTemplate.update(
			"insert into tb_notification (user_id, type, title, link, created_at) values (?, ?, ?, ?, ?)", 2L,
			"SYSTEM", title, "/notices", createdAt);
	}

	private void refreshToken(String tokenHash, LocalDateTime expiresAt) {
		jdbcTemplate.update("insert into tb_refresh_token (user_id, family_id, token_hash, expires_at, created_at, "
			+ "updated_at) values (2, '00000000-0000-0000-0000-000000000000', ?, ?, now(6), now(6))", tokenHash,
			expiresAt);
	}

	private MvcTestResult asAdmin(MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, superAdmin()).exchange();
	}

	private MvcTestResult asOperator(MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(OPERATOR_ID, "ADMIN")).exchange();
	}

	private String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
