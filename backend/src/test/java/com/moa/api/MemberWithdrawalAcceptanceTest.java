package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
import org.springframework.web.util.UriComponentsBuilder;

import com.jayway.jsonpath.JsonPath;
import com.moa.api.application.v1.notification.NotificationApplication;
import com.moa.api.dto.notification.NotificationType;
import com.moa.common.security.Role;
import com.moa.common.security.recovery.RecoveryCookies;
import com.moa.common.security.recovery.RecoveryTicket;
import com.moa.support.AcceptanceTest;
import com.moa.support.CodeSeed;
import com.moa.support.DatabaseCleaner;
import com.moa.support.FakeOAuthProviderClient;
import com.moa.support.TestUsers;

import jakarta.servlet.http.Cookie;

/**
 * 회원 탈퇴와 복구 (specs/018-member-withdrawal US1·US2, contracts/withdrawal-api.md).
 * 화면에서만 보이는 AC(US1-AC1·AC2·AC3·AC9, US2-AC5)는 프론트 단위 테스트가, 파기(US2-AC6)는 MemberPurgeAcceptanceTest가 본다.
 * 시각은 DB의 now()가 아니라 서버(JVM)의 지금으로 넣는다. 서버는 자기 시간대의 LocalDateTime으로 저장하고 비교한다.
 */
class MemberWithdrawalAcceptanceTest extends AcceptanceTest {

	private static final String WITHDRAWAL = "/api/v1/users/me/withdrawal";
	private static final String LOGIN = "/api/v1/auth/login";
	private static final String REFRESH = "/api/v1/auth/token/refresh";
	private static final String RECOVERY = "/api/v1/auth/recovery";
	private static final String ME = "/api/v1/users/me";
	private static final String PROFILE = "/api/v1/users/me/profile";
	private static final String ADMIN_USERS = "/api/v1/admin/users";
	private static final String NOTICES = "/api/v1/notices";
	private static final String OAUTH = "/api/v1/auth/oauth";
	private static final String RECOVERY_COOKIE = RecoveryCookies.COOKIE;
	private static final long ADMIN_ID = 2L;
	private static final long MEMBER_ID = 10L;
	private static final long OTHER_ID = 11L;
	private static final String LOGIN_ID = "moa_kim";
	private static final String PASSWORD = "member1234!";

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private CodeSeed codeSeed;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DatabaseCleaner databaseCleaner;

	@Autowired
	private NotificationApplication notificationApplication;

	@Autowired
	private RecoveryCookies recoveryCookies;

	@Autowired
	private FakeOAuthProviderClient provider;

	private String member;

	@BeforeEach
	void setUp() {
		databaseCleaner.clean();
		codeSeed.restore();
		testUsers.create(1L, "system", null, Role.USER);
		testUsers.create(MEMBER_ID, LOGIN_ID, PASSWORD, Role.USER);
		jdbcTemplate.update("insert into tb_user_profile (user_id, nickname_key, nickname_tag, created_at, updated_at) "
			+ "values (?, ?, '0421', now(6), now(6))", MEMBER_ID, LOGIN_ID);
		member = bearer(MEMBER_ID, "USER");
	}

	@AfterEach
	void resetProvider() {
		provider.reset();
	}

	// ---------- US1 탈퇴 ----------

	@Test
	@DisplayName("US1-AC4 닉네임이 다른 탈퇴 요청은 화면을 거치지 않아도 V001(칸 nickname)로 막히고 탈퇴되지 않는다. 대소문자도 다르면 막힌다")
	void nicknameMismatch() {
		for (String wrong : List.of("다른닉네임", "MOA_KIM", "")) {
			MvcTestResult result = withdraw(member, wrong);

			assertThat(result).hasStatus(400);
			assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("V001");
			assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("nickname");
		}
		assertThat(withdrawnAt(MEMBER_ID)).isNull();
	}

	@Test
	@DisplayName("US1-AC5 닉네임이 같으면(앞뒤 공백 무시) 탈퇴되고, 리프레시 쿠키가 지워지며 30일 뒤의 완전 삭제 예정 시각을 준다")
	void withdrawSucceeds() {
		MvcTestResult result = withdraw(member, "  " + LOGIN_ID + " ");

		assertThat(result).hasStatus(200);
		OffsetDateTime purgeAt = OffsetDateTime.parse(JsonPath.read(body(result), "$.purgeAt"));
		assertThat(purgeAt.toLocalDateTime()).isCloseTo(LocalDateTime.now().plusDays(30),
			within(1, ChronoUnit.MINUTES));
		Cookie cleared = result.getResponse().getCookie("refresh_token");
		assertThat(cleared).isNotNull();
		assertThat(cleared.getMaxAge()).isZero();
		assertThat(withdrawnAt(MEMBER_ID)).isNotNull();
	}

	@Test
	@DisplayName("US1-AC6 다른 브라우저의 다음 요청부터 막힌다: 로그인이 필요한 API는 A002, 재발급은 A003")
	void otherSessionsAreBlocked() {
		Cookie otherBrowserRefresh = login(LOGIN_ID, PASSWORD).getResponse().getCookie("refresh_token");
		assertThat(get(ME, member)).hasStatus(200);

		assertThat(withdraw(member, LOGIN_ID)).hasStatus(200);

		MvcTestResult me = get(ME, member);
		assertThat(me).hasStatus(401);
		assertThat(me).bodyJson().extractingPath("$.code").isEqualTo("A002");
		MvcTestResult refresh = mvc.post().uri(REFRESH).cookie(otherBrowserRefresh).exchange();
		assertThat(refresh).hasStatus(401);
		assertThat(refresh).bodyJson().extractingPath("$.code").isEqualTo("A003");
	}

	@Test
	@DisplayName("Edge 탈퇴 뒤 옛 토큰으로 로그인 없이 되는 API를 부르면 로그인하지 않은 것으로 답한다")
	void publicApiAfterWithdrawal() {
		assertThat(withdraw(member, LOGIN_ID)).hasStatus(200);

		assertThat(get(NOTICES, member)).hasStatus(200);
	}

	@Test
	@DisplayName("US1-AC7 탈퇴한 회원은 기본 조회에 없다: 관리자 상세는 R001, 이름은 \"탈퇴한 회원\"으로 보인다(MemberDisplayTest)")
	void withdrawnMemberIsNotFound() {
		String admin = superAdmin();
		assertThat(withdraw(member, LOGIN_ID)).hasStatus(200);

		MvcTestResult detail = get(ADMIN_USERS + "/" + MEMBER_ID, admin);
		assertThat(detail).hasStatus(404);
		assertThat(detail).bodyJson().extractingPath("$.code").isEqualTo("R001");
	}

	@Test
	@DisplayName("US1-AC8 유예 중에는 프로필·시간대·게임 정보·소셜 연결·약관 동의가 그대로 있고 파기 이력은 없다")
	void dataIsKeptDuringGrace() {
		ownedRows(MEMBER_ID);
		int before = ownedRowCount(MEMBER_ID);

		assertThat(withdraw(member, LOGIN_ID)).hasStatus(200);

		assertThat(ownedRowCount(MEMBER_ID)).isEqualTo(before).isPositive();
		assertThat(count("select count(*) from tb_purge_log")).isZero();
	}

	@Test
	@DisplayName("US1-AC10 관리자 계정은 A201로 막히고 아무것도 바뀌지 않는다")
	void adminCannotWithdraw() {
		testUsers.create(ADMIN_ID, "admin", "admin1234!", Role.ADMIN);

		MvcTestResult result = withdraw(bearer(ADMIN_ID, "ADMIN"), "admin");

		assertThat(result).hasStatus(403);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("A201");
		assertThat(withdrawnAt(ADMIN_ID)).isNull();
	}

	@Test
	@DisplayName("US1-AC11 탈퇴하면 그 회원의 푸시 구독(브라우저 둘)이 모두 지워지고, 복구해도 돌아오지 않는다. 다른 회원 것은 그대로다")
	void pushSubscriptionsAreDeleted() {
		testUsers.create(OTHER_ID, "other", PASSWORD, Role.USER);
		pushSubscription(MEMBER_ID, "https://push.test/a");
		pushSubscription(MEMBER_ID, "https://push.test/b");
		pushSubscription(OTHER_ID, "https://push.test/c");

		assertThat(withdraw(member, LOGIN_ID)).hasStatus(200);
		assertThat(count("select count(*) from tb_push_subscription where user_id = ?", MEMBER_ID)).isZero();

		assertThat(recover(login(LOGIN_ID, PASSWORD).getResponse().getCookie(RECOVERY_COOKIE))).hasStatus(200);
		assertThat(count("select count(*) from tb_push_subscription where user_id = ?", MEMBER_ID)).isZero();
		assertThat(count("select count(*) from tb_push_subscription where user_id = ?", OTHER_ID)).isOne();
	}

	@Test
	@DisplayName("US1-AC12 유예 중인 회원에게 개인 알림을 보내면 저장하지 않고 null을 돌려준다(보낸 쪽은 실패하지 않는다)")
	void noNotificationDuringGrace() {
		assertThat(withdraw(member, LOGIN_ID)).hasStatus(200);

		assertThat(notificationApplication.notify(MEMBER_ID, NotificationType.SYSTEM, "제목", "/")).isNull();
		assertThat(count("select count(*) from tb_notification where user_id = ?", MEMBER_ID)).isZero();
	}

	@Test
	@DisplayName("US1-AC13 유예 중인 회원은 사용자 관리 목록에 없고, 복구하면 다시 나온다")
	void adminListHidesWithdrawnMember() {
		String admin = superAdmin();
		assertThat(adminListLoginIds(admin)).contains(LOGIN_ID);

		assertThat(withdraw(member, LOGIN_ID)).hasStatus(200);
		assertThat(adminListLoginIds(admin)).doesNotContain(LOGIN_ID);

		assertThat(recover(login(LOGIN_ID, PASSWORD).getResponse().getCookie(RECOVERY_COOKIE))).hasStatus(200);
		assertThat(adminListLoginIds(admin)).contains(LOGIN_ID);
	}

	@Test
	@DisplayName("Edge 두 브라우저에서 동시에 탈퇴해도 탈퇴는 한 번이고, 둘 다 처음 시각의 완전 삭제 예정 시각을 받는다")
	void concurrentWithdrawals() throws Exception {
		List<MvcTestResult> results = concurrently(() -> withdraw(member, LOGIN_ID), () -> withdraw(member, LOGIN_ID));

		assertThat(results).allSatisfy(result -> assertThat(result).hasStatus(200));
		assertThat(results.stream().map(result -> (String)JsonPath.read(body(result), "$.purgeAt")).distinct())
			.hasSize(1);
	}

	// ---------- US2 복구 ----------

	@Test
	@DisplayName("US2-AC1 유예 중인 소셜 회원이 로그인하면 복구 쿠키와 함께 복구 화면(next 붙음)으로 가고, 새 회원은 생기지 않는다")
	void socialLoginShowsRecovery() {
		long socialId = socialMember("google-sub-0001", "kim@example.com");
		assertThat(withdraw(bearer(socialId, "USER"), nameOf(socialId))).hasStatus(200);
		int users = count("select count(*) from tb_user");

		MvcTestResult callback = socialCallback("code-again", "google-sub-0001", "kim@example.com", "/parties");

		assertThat(location(callback)).isEqualTo("/sign-in/recover?next=/parties");
		Cookie cookie = callback.getResponse().getCookie(RECOVERY_COOKIE);
		assertThat(cookie).isNotNull();
		assertThat(callback.getResponse().getCookie("refresh_token")).isNull();
		assertThat(getRecovery(cookie)).hasStatus(200);
		assertThat(count("select count(*) from tb_user")).isEqualTo(users);
		assertThat(lastLoginFailure()).isEqualTo("A401");
	}

	@Test
	@DisplayName("US2-AC2 유예 중인 비밀번호 회원이 맞는 비밀번호로 로그인하면 A401과 복구 쿠키를 받고, 안내에 완전 삭제 예정 시각이 있다")
	void passwordLoginShowsRecovery() {
		assertThat(withdraw(member, LOGIN_ID)).hasStatus(200);

		MvcTestResult result = login(LOGIN_ID, PASSWORD);

		assertThat(result).hasStatus(401);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("A401");
		assertThat(result.getResponse().getCookie("refresh_token")).isNull();
		Cookie cookie = result.getResponse().getCookie(RECOVERY_COOKIE);
		assertThat(cookie).isNotNull();
		MvcTestResult recovery = getRecovery(cookie);
		assertThat(recovery).hasStatus(200);
		assertThat(OffsetDateTime.parse(JsonPath.read(body(recovery), "$.purgeAt")).toLocalDateTime())
			.isCloseTo(withdrawnAt(MEMBER_ID).plusDays(30), within(1, ChronoUnit.SECONDS));
		assertThat(lastLoginFailure()).isEqualTo("A401");
	}

	@Test
	@DisplayName("US2-AC3 틀린 비밀번호면 유예 중·파기 대기여도 탈퇴하지 않은 회원·없는 아이디와 똑같은 A006이고, 쿠키도 없고 접속 이력도 A006이다")
	void wrongPasswordRevealsNothing() {
		testUsers.create(OTHER_ID, "active_member", PASSWORD, Role.USER);
		testUsers.create(12L, "pending_member", PASSWORD, Role.USER);
		withdrawnAt(12L, LocalDateTime.now().minusDays(31));
		assertThat(withdraw(member, LOGIN_ID)).hasStatus(200);

		for (String loginId : List.of(LOGIN_ID, "pending_member", "active_member", "nobody")) {
			MvcTestResult result = login(loginId, "wrong-password!");

			assertThat(result).hasStatus(401);
			assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("A006");
			assertThat(result.getResponse().getCookie(RECOVERY_COOKIE)).isNull();
			assertThat(lastLoginFailure()).isEqualTo("A006");
		}
	}

	@Test
	@DisplayName("US2-AC4 복구하면 로그인되고(토큰, 리프레시 쿠키) 복구 쿠키가 지워지며 닉네임·태그·프로필·소셜 연결이 탈퇴 전과 같다")
	void recoverRestoresEverything() {
		ownedRows(MEMBER_ID);
		String profileBefore = body(get(PROFILE, member));
		int rowsBefore = ownedRowCount(MEMBER_ID);
		assertThat(withdraw(member, LOGIN_ID)).hasStatus(200);

		MvcTestResult recovered = recover(login(LOGIN_ID, PASSWORD).getResponse().getCookie(RECOVERY_COOKIE));

		assertThat(recovered).hasStatus(200);
		assertThat(recovered.getResponse().getCookie("refresh_token")).isNotNull();
		assertThat(recovered.getResponse().getCookie(RECOVERY_COOKIE).getMaxAge()).isZero();
		String accessToken = JsonPath.read(body(recovered), "$.accessToken");
		assertThat(get(ME, "Bearer " + accessToken)).hasStatus(200);
		assertThat(body(get(PROFILE, "Bearer " + accessToken))).isEqualTo(profileBefore);
		assertThat(ownedRowCount(MEMBER_ID)).isEqualTo(rowsBefore);
		assertThat(withdrawnAt(MEMBER_ID)).isNull();
		assertThat(count("select count(*) from tb_login_history where user_id = ? and success = true", MEMBER_ID))
			.isOne();
	}

	@Test
	@DisplayName("US2-AC7 복구한 뒤 다시 탈퇴하면 완전 삭제 예정 시각은 다시 탈퇴한 때부터 30일 뒤다")
	void withdrawAgainRestartsGrace() {
		testUsers.create(OTHER_ID, "again", PASSWORD, Role.USER);
		withdrawnAt(OTHER_ID, LocalDateTime.now().minusDays(20));
		assertThat(recover(login("again", PASSWORD).getResponse().getCookie(RECOVERY_COOKIE))).hasStatus(200);

		MvcTestResult again = withdraw(bearer(OTHER_ID, "USER"), "again");

		assertThat(OffsetDateTime.parse(JsonPath.read(body(again), "$.purgeAt")).toLocalDateTime())
			.isCloseTo(LocalDateTime.now().plusDays(30), within(1, ChronoUnit.MINUTES));
	}

	@Test
	@DisplayName("US2-AC8 잠긴 유예 중 회원은 잠긴 회원과 똑같이 A007이고 복구 쿠키가 없다. 잠기기 전에 받은 쿠키로도 복구되지 않는다(A007)")
	void lockedMemberCannotRecover() {
		assertThat(withdraw(member, LOGIN_ID)).hasStatus(200);
		Cookie before = login(LOGIN_ID, PASSWORD).getResponse().getCookie(RECOVERY_COOKIE);
		jdbcTemplate.update("update tb_user set locked_at = ? where id = ?", LocalDateTime.now(), MEMBER_ID);

		MvcTestResult result = login(LOGIN_ID, PASSWORD);

		assertThat(result).hasStatus(401);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("A007");
		assertThat(result.getResponse().getCookie(RECOVERY_COOKIE)).isNull();
		assertThat(recover(before)).hasStatus(401).bodyJson().extractingPath("$.code").isEqualTo("A007");
		assertThat(withdrawnAt(MEMBER_ID)).isNotNull();
	}

	@Test
	@DisplayName("US2-AC9 유예가 지났으면 로그인은 A402, 소셜은 purgePending, 복구 요청도 A402다")
	void purgePendingCannotLogInOrRecover() {
		long socialId = socialMember("google-sub-0002", "lee@example.com");
		LocalDateTime longAgo = LocalDateTime.now().minusDays(31).truncatedTo(ChronoUnit.MICROS);
		withdrawnAt(MEMBER_ID, longAgo);
		withdrawnAt(socialId, longAgo);

		MvcTestResult login = login(LOGIN_ID, PASSWORD);
		assertThat(login).hasStatus(401);
		assertThat(login).bodyJson().extractingPath("$.code").isEqualTo("A402");
		assertThat(login.getResponse().getCookie(RECOVERY_COOKIE)).isNull();
		assertThat(location(socialCallback("code-late", "google-sub-0002", "lee@example.com", null)))
			.isEqualTo("/sign-in?oauthError=purgePending");

		Cookie ticket = ticket(MEMBER_ID, longAgo);
		assertThat(getRecovery(ticket)).hasStatus(401).bodyJson().extractingPath("$.code").isEqualTo("A402");
		assertThat(recover(ticket)).hasStatus(401).bodyJson().extractingPath("$.code").isEqualTo("A402");
		assertThat(withdrawnAt(MEMBER_ID)).isNotNull();
	}

	@Test
	@DisplayName("US2-AC10 쿠키 없음·위조·다시 탈퇴한 뒤의 옛 쿠키·이미 쓴 쿠키로는 복구되지 않는다(A403)")
	void recoveryNeedsValidCookie() {
		assertThat(withdraw(member, LOGIN_ID)).hasStatus(200);
		Cookie old = login(LOGIN_ID, PASSWORD).getResponse().getCookie(RECOVERY_COOKIE);

		assertThat(getRecovery(null)).hasStatus(400).bodyJson().extractingPath("$.code").isEqualTo("A403");
		assertThat(recover(null)).hasStatus(400).bodyJson().extractingPath("$.code").isEqualTo("A403");
		assertThat(recover(new Cookie(RECOVERY_COOKIE, "forged.token.value"))).hasStatus(400);
		assertThat(recover(ticket(MEMBER_ID, LocalDateTime.now().minusDays(1)))).hasStatus(400);

		assertThat(recover(old)).hasStatus(200);
		assertThat(recover(old)).hasStatus(400).bodyJson().extractingPath("$.code").isEqualTo("A403");

		withdrawnAt(MEMBER_ID, LocalDateTime.now().minusMinutes(1));
		assertThat(recover(old)).hasStatus(400).bodyJson().extractingPath("$.code").isEqualTo("A403");
		assertThat(withdrawnAt(MEMBER_ID)).isNotNull();
	}

	@Test
	@DisplayName("US2-AC11 관리자가 지운 회원은 지금처럼 로그인되지 않고(A006) 복구 안내도 복구도 없다(A403)")
	void adminDeletedMemberCannotRecover() {
		LocalDateTime withdrawn = LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.MICROS);
		withdrawnAt(MEMBER_ID, withdrawn);
		jdbcTemplate.update("update tb_user set deleted_at = ?, deleted_by = 1 where id = ?", LocalDateTime.now(),
			MEMBER_ID);

		MvcTestResult login = login(LOGIN_ID, PASSWORD);
		assertThat(login).hasStatus(401);
		assertThat(login).bodyJson().extractingPath("$.code").isEqualTo("A006");
		assertThat(login.getResponse().getCookie(RECOVERY_COOKIE)).isNull();
		assertThat(recover(ticket(MEMBER_ID, withdrawn))).hasStatus(400).bodyJson().extractingPath("$.code")
			.isEqualTo("A403");
	}

	@Test
	@DisplayName("US2-AC12 유예 중인 회원의 로그인 아이디로 새 계정을 만들면 R002로 막히고, 그 회원은 복구할 수 있다")
	void loginIdIsHeld() {
		String admin = superAdmin();
		assertThat(withdraw(member, LOGIN_ID)).hasStatus(200);

		MvcTestResult created = mvc.post().uri(ADMIN_USERS).header(HttpHeaders.AUTHORIZATION, admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"loginId\":\"" + LOGIN_ID + "\",\"name\":\"새 계정\",\"role\":\"ADMIN\"}").exchange();

		assertThat(created).hasStatus(409);
		assertThat(created).bodyJson().extractingPath("$.code").isEqualTo("R002");
		assertThat(recover(login(LOGIN_ID, PASSWORD).getResponse().getCookie(RECOVERY_COOKIE))).hasStatus(200);
	}

	@Test
	@DisplayName("US2-AC13 유예 중인 소셜 회원의 이메일로 다른 소셜 계정이 가입하면 emailInUse로 막히고, 그 회원은 복구할 수 있다")
	void emailIsHeld() {
		long socialId = socialMember("google-sub-0003", "park@example.com");
		assertThat(withdraw(bearer(socialId, "USER"), nameOf(socialId))).hasStatus(200);

		assertThat(location(socialCallback("code-other", "google-sub-9999", "park@example.com", null)))
			.isEqualTo("/sign-in?oauthError=emailInUse");

		MvcTestResult again = socialCallback("code-back", "google-sub-0003", "park@example.com", null);
		assertThat(recover(again.getResponse().getCookie(RECOVERY_COOKIE))).hasStatus(200);
	}

	// ---------- 도우미 ----------

	private MvcTestResult withdraw(String bearer, String nickname) {
		return mvc.post().uri(WITHDRAWAL).header(HttpHeaders.AUTHORIZATION, bearer)
			.contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"" + nickname + "\"}").exchange();
	}

	private MvcTestResult login(String loginId, String password) {
		return mvc.post().uri(LOGIN).contentType(MediaType.APPLICATION_JSON)
			.content("{\"loginId\":\"" + loginId + "\",\"password\":\"" + password + "\"}").exchange();
	}

	private MvcTestResult getRecovery(Cookie cookie) {
		return cookie == null ? mvc.get().uri(RECOVERY).exchange() : mvc.get().uri(RECOVERY).cookie(cookie).exchange();
	}

	private MvcTestResult recover(Cookie cookie) {
		return cookie == null ? mvc.post().uri(RECOVERY).exchange()
			: mvc.post().uri(RECOVERY).cookie(cookie).exchange();
	}

	private MvcTestResult get(String uri, String bearer) {
		return mvc.get().uri(uri).header(HttpHeaders.AUTHORIZATION, bearer).exchange();
	}

	/** 서버가 서명한 복구 쿠키 (본인 확인 없이 만들 수 없는 값. 시각을 앞당긴 회원의 쿠키를 만들 때 쓴다) */
	private Cookie ticket(long userId, LocalDateTime withdrawnAt) {
		MockHttpServletResponse response = new MockHttpServletResponse();
		recoveryCookies.write(response, new RecoveryTicket(userId, withdrawnAt));
		return response.getCookie(RECOVERY_COOKIE);
	}

	/** 구글로 처음 가입하고 회원 ID를 돌려준다 */
	private long socialMember(String subject, String email) {
		provider.register("code-" + subject, subject, email, true, "Provider Name");
		Cookie signup = socialCallback("code-" + subject, subject, email, null).getResponse().getCookie("oauth_signup");
		MvcTestResult signedUp = mvc.post().uri(OAUTH + "/sign-up").cookie(signup)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nickname\":\"소셜" + subject.substring(subject.length() - 4) + "\",\"agreements\":["
				+ "{\"type\":\"SERVICE\",\"version\":\"draft\"},{\"type\":\"PRIVACY\",\"version\":\"draft\"}]}")
			.exchange();
		assertThat(signedUp).hasStatus(200);
		Number id = JsonPath.read(body(get(ME, "Bearer " + JsonPath.read(body(signedUp), "$.accessToken"))), "$.id");
		return id.longValue();
	}

	private MvcTestResult socialCallback(String code, String subject, String email, String next) {
		provider.register(code, subject, email, true, "Provider Name");
		MvcTestResult authorize = next == null ? mvc.get().uri(OAUTH + "/google/authorize").exchange()
			: mvc.get().uri(OAUTH + "/google/authorize?next={next}", next).exchange();
		String state = UriComponentsBuilder.fromUriString(location(authorize)).build().getQueryParams()
			.getFirst("state");
		MvcTestResult callback = mvc.get().uri(OAUTH + "/google/callback?code={code}&state={state}", code, state)
			.cookie(authorize.getResponse().getCookie("oauth_state")).exchange();
		assertThat(callback).hasStatus(302);
		return callback;
	}

	private void ownedRows(long userId) {
		jdbcTemplate.update("insert into tb_user_profile_play_time (user_id, play_time_code) values (?, 'DAWN')",
			userId);
		jdbcTemplate.update("insert into tb_user_game_profile (user_id, game_code, game_account_id, created_at, "
			+ "updated_at, created_by, updated_by) values (?, 'LOL', 'game-id', now(6), now(6), 1, 1)", userId);
		jdbcTemplate.update("insert into tb_user_social_account (user_id, provider, subject, email, display_name, "
			+ "created_at) values (?, 'GOOGLE', ?, ?, 'Name', now(6))", userId, "sub-" + userId,
			userId + "@example.com");
		jdbcTemplate.update("insert into tb_user_terms_agreement (user_id, terms_type, terms_version, agreed_at) "
			+ "values (?, 'SERVICE', 'draft', now(6))", userId);
	}

	private int ownedRowCount(long userId) {
		int total = 0;
		for (String table : List.of("tb_user_profile", "tb_user_profile_play_time", "tb_user_game_profile",
			"tb_user_social_account", "tb_user_terms_agreement")) {
			total += count("select count(*) from " + table + " where user_id = ?", userId);
		}
		return total;
	}

	private void pushSubscription(long userId, String endpoint) {
		jdbcTemplate.update("insert into tb_push_subscription (user_id, endpoint, p256dh, auth, created_at) "
			+ "values (?, ?, 'p256dh', 'auth', now(6))", userId, endpoint);
	}

	private List<String> adminListLoginIds(String admin) {
		return JsonPath.read(body(mvc.get().uri(ADMIN_USERS + "?size=100").header(HttpHeaders.AUTHORIZATION, admin)
			.exchange()), "$.content[*].loginId");
	}

	private LocalDateTime withdrawnAt(long userId) {
		return jdbcTemplate.queryForObject("select withdrawn_at from tb_user where id = ?", LocalDateTime.class,
			userId);
	}

	private void withdrawnAt(long userId, LocalDateTime at) {
		jdbcTemplate.update("update tb_user set withdrawn_at = ? where id = ?", at, userId);
	}

	private String nameOf(long userId) {
		return jdbcTemplate.queryForObject("select name from tb_user where id = ?", String.class, userId);
	}

	private String lastLoginFailure() {
		return jdbcTemplate.queryForObject("select failure_code from tb_login_history order by id desc limit 1",
			String.class);
	}

	private int count(String sql, Object... args) {
		return jdbcTemplate.queryForObject(sql, Integer.class, args);
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
	private List<MvcTestResult> concurrently(Callable<MvcTestResult>... tasks) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(tasks.length);
		try {
			CountDownLatch start = new CountDownLatch(1);
			List<Future<MvcTestResult>> futures = new java.util.ArrayList<>();
			for (Callable<MvcTestResult> task : tasks) {
				futures.add(executor.submit(() -> {
					start.await();
					return task.call();
				}));
			}
			start.countDown();
			List<MvcTestResult> results = new java.util.ArrayList<>();
			for (Future<MvcTestResult> future : futures) {
				results.add(future.get());
			}
			return results;
		} finally {
			executor.shutdown();
		}
	}
}
