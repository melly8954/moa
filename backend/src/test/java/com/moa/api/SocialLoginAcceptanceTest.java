package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.util.UriComponentsBuilder;

import com.jayway.jsonpath.JsonPath;
import com.moa.common.security.Role;
import com.moa.support.AcceptanceTest;
import com.moa.support.DatabaseCleaner;
import com.moa.support.FakeOAuthProviderClient;
import com.moa.support.TestUsers;

import jakarta.servlet.http.Cookie;

/**
 * 소셜 로그인 (specs/010-social-login US1~US3, 구글). 제공자는 FakeOAuthProviderClient로 바꿔 부르지 않는다.
 * 브라우저 흐름은 authorize(302, oauth_state 쿠키) → callback(302) → sign-up(토큰) 순서로 쿠키를 이어 붙여 흉내 낸다.
 */
class SocialLoginAcceptanceTest extends AcceptanceTest {

	private static final String BASE = "/api/v1/auth/oauth";
	private static final String AUTHORIZE = BASE + "/google/authorize";
	private static final String CALLBACK = BASE + "/google/callback";
	private static final String SIGN_UP = BASE + "/sign-up";
	private static final String ME = "/api/v1/users/me";
	private static final String TERMS = "draft";
	private static final long ADMIN_ID = 2L;

	@Autowired
	private FakeOAuthProviderClient provider;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DatabaseCleaner databaseCleaner;

	/** 로컬 계정 SQL의 admin·user가 남아 있을 수 있어 비우고 시작한다 */
	@BeforeEach
	void setUp() {
		databaseCleaner.clean();
		testUsers.create(1L, "system", null, Role.USER);
		testUsers.create(ADMIN_ID, "admin", "admin1234!", Role.ADMIN);
		// ADMIN은 관리자 계정일 뿐이라 모든 권한은 최고 관리자 역할이 준다 (specs/003-admin-roles research R21)
		testUsers.makeSuperAdmin(ADMIN_ID);
	}

	@AfterEach
	void resetProvider() {
		provider.reset();
	}

	// ---------- US1 처음 온 사람이 소셜 계정으로 가입한다 ----------

	@Test
	@DisplayName("US1-AC1 가입한 적 없는 구글 계정으로 닉네임·약관을 마치면 회원이 생기고 로그인된다")
	void signUpCreatesMemberAndLogsIn() {
		provider.register("code-1", "google-sub-0001", "kim@example.com", true, "Kim Google");

		MvcTestResult callback = callback(authorize(null), "code-1");
		MvcTestResult signUp = signUp(callback.getResponse().getCookie("oauth_signup"), "모아킴", TERMS, TERMS);

		assertThat(signUp).hasStatus(200);
		assertThat(signUp.getResponse().getCookie("refresh_token")).isNotNull();
		MvcTestResult me = me(accessToken(signUp));
		assertThat(me).hasStatus(200);
		assertThat(me).bodyJson().extractingPath("$.role").isEqualTo("USER");
		assertThat(socialMemberCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("US1-AC2 관리자에게 소셜 회원은 USER, 비밀번호 없는 계정으로 보이고 비밀번호 초기화를 할 수 없다")
	void adminSeesPasswordlessUser() {
		Long userId = signUpMember("code-1", "google-sub-0001", "kim@example.com", "모아킴");
		String admin = bearer(ADMIN_ID, "ADMIN");

		MvcTestResult detail = mvc.get().uri("/api/v1/admin/users/" + userId).header(HttpHeaders.AUTHORIZATION, admin)
			.exchange();

		assertThat(detail).hasStatus(200);
		assertThat(detail).bodyJson().extractingPath("$.role").isEqualTo("USER");
		assertThat(detail).bodyJson().extractingPath("$.hasPassword").isEqualTo(false);
		assertThat(mvc.post().uri("/api/v1/admin/users/" + userId + "/password-reset")
			.header(HttpHeaders.AUTHORIZATION, admin).exchange())
			.hasStatus(409).bodyJson().extractingPath("$.code").isEqualTo("B001");
	}

	@Test
	@DisplayName("US1-AC3 처음 소셜 로그인하면 닉네임 화면으로 가고, 가입하면 그 닉네임이 표시 이름이다")
	void callbackSendsToNicknameScreenAndNicknameIsDisplayName() {
		provider.register("code-1", "google-sub-0001", "kim@example.com", true, "Kim Google");

		MvcTestResult callback = callback(authorize(null), "code-1");

		assertThat(location(callback)).isEqualTo("/sign-up/nickname");
		MvcTestResult signUp = signUp(callback.getResponse().getCookie("oauth_signup"), "모아킴", TERMS, TERMS);
		assertThat(me(accessToken(signUp))).bodyJson().extractingPath("$.name").isEqualTo("모아킴");
	}

	@Test
	@DisplayName("US1-AC4 로그인이 필요한 주소에서 왔으면 가입 화면과 그 뒤에도 그 주소를 돌아갈 곳으로 넘긴다")
	void nextPathIsKept() {
		provider.register("code-1", "google-sub-0001", "kim@example.com", true, "Kim Google");

		MvcTestResult callback = callback(authorize("/notices"), "code-1");

		assertThat(location(callback)).isEqualTo("/sign-up/nickname?next=/notices");
	}

	@Test
	@DisplayName("US1-AC5 닉네임 화면에서 떠나면 회원이 없고, 다시 소셜 로그인하면 닉네임 화면부터 다시 한다")
	void leavingNicknameScreenCreatesNothing() {
		provider.register("code-1", "google-sub-0001", "kim@example.com", true, "Kim Google");
		provider.register("code-2", "google-sub-0001", "kim@example.com", true, "Kim Google");

		MvcTestResult first = callback(authorize(null), "code-1");
		MvcTestResult again = callback(authorize(null), "code-2");

		assertThat(location(first)).isEqualTo("/sign-up/nickname");
		assertThat(location(again)).isEqualTo("/sign-up/nickname");
		assertThat(socialMemberCount()).isZero();
	}

	@Test
	@DisplayName("US1-AC6 소셜 회원에게 시스템 역할 ADMIN을 주면 A011, 업무 역할은 회원이라 B001로 막힌다")
	void socialMemberCannotGetManagementRoles() {
		Long userId = signUpMember("code-1", "google-sub-0001", "kim@example.com", "모아킴");
		String admin = bearer(ADMIN_ID, "ADMIN");
		MvcTestResult role = mvc.post().uri("/api/v1/admin/roles").header(HttpHeaders.AUTHORIZATION, admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"SECURITY\",\"name\":\"보안 담당\",\"permissions\":[\"LOGIN_HISTORY_READ\"]}")
			.exchange();
		Number roleId = JsonPath.read(body(role), "$.id");

		assertThat(mvc.put().uri("/api/v1/admin/users/" + userId + "/role").header(HttpHeaders.AUTHORIZATION, admin)
			.contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}").exchange())
			.hasStatus(409).bodyJson().extractingPath("$.code").isEqualTo("A011");
		assertThat(mvc.post().uri("/api/v1/admin/roles/" + roleId + "/members").header(HttpHeaders.AUTHORIZATION, admin)
			.contentType(MediaType.APPLICATION_JSON).content("{\"userIds\":[" + userId + "]}").exchange())
			.hasStatus(409).bodyJson().extractingPath("$.code").isEqualTo("B001");
		assertThat(mvc.put().uri("/api/v1/admin/users/" + userId + "/roles").header(HttpHeaders.AUTHORIZATION, admin)
			.contentType(MediaType.APPLICATION_JSON).content("{\"roleIds\":[" + roleId + "]}").exchange())
			.hasStatus(409).bodyJson().extractingPath("$.code").isEqualTo("B001");
	}

	@Test
	@DisplayName("US1-AC7 필수 약관 하나라도 동의하지 않으면 가입되지 않는다 (A017)")
	void termsRequired() {
		provider.register("code-1", "google-sub-0001", "kim@example.com", true, "Kim Google");
		Cookie signup = callback(authorize(null), "code-1").getResponse().getCookie("oauth_signup");

		MvcTestResult result = mvc.post().uri(SIGN_UP).cookie(signup).contentType(MediaType.APPLICATION_JSON)
			.content("{\"nickname\":\"모아킴\",\"agreements\":[{\"type\":\"SERVICE\",\"version\":\"draft\"}]}")
			.exchange();

		assertThat(result).hasStatus(400).bodyJson().extractingPath("$.code").isEqualTo("A017");
		assertThat(socialMemberCount()).isZero();
	}

	@Test
	@DisplayName("US1-AC8 가입하면 이용약관·개인정보 각각의 동의 시각과 버전이 남는다")
	void termsAgreementsRecorded() {
		Long userId = signUpMember("code-1", "google-sub-0001", "kim@example.com", "모아킴");

		List<String> rows = jdbcTemplate.queryForList("select concat(terms_type, ':', terms_version) "
			+ "from tb_user_terms_agreement where user_id = ? and agreed_at is not null order by terms_type",
			String.class,
			userId);

		assertThat(rows).containsExactly("PRIVACY:draft", "SERVICE:draft");
	}

	@Test
	@DisplayName("US1-AC9 다른 회원이 쓰는 이메일의 소셜 계정은 가입이 막히고 회원이 생기지 않는다")
	void emailInUseBlocksSignUp() {
		signUpMember("code-1", "google-sub-0001", "kim@example.com", "모아킴");
		provider.register("code-2", "google-sub-0002", " Kim@Example.com ", true, "Other");

		MvcTestResult callback = callback(authorize(null), "code-2");

		assertThat(location(callback)).isEqualTo("/sign-in?oauthError=emailInUse");
		assertThat(callback.getResponse().getCookie("oauth_signup")).isNull();
		assertThat(socialMemberCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("US1-AC12 제공자가 이메일을 주지 않거나 인증하지 않았으면 가입이 막히고 회원이 생기지 않는다")
	void unverifiedEmailBlocksSignUp() {
		provider.register("code-1", "google-sub-0001", "kim@example.com", false, "Kim");
		provider.register("code-2", "google-sub-0002", null, true, "Lee");

		assertThat(location(callback(authorize(null), "code-1"))).isEqualTo("/sign-in?oauthError=emailUnverified");
		assertThat(location(callback(authorize(null), "code-2"))).isEqualTo("/sign-in?oauthError=emailUnverified");
		assertThat(socialMemberCount()).isZero();
	}

	@Test
	@DisplayName("US1-AC13 다른 회원과 같은 닉네임(대소문자만 다른 것 포함)으로도 가입되고 서로 다른 태그를 받는다")
	void sameNicknameGetsDifferentTag() {
		Long first = signUpMember("code-1", "google-sub-0001", "kim@example.com", "Moa_Kim");
		Long second = signUpMember("code-2", "google-sub-0002", "lee@example.com", "moa_kim");

		List<String> keys = jdbcTemplate.queryForList(
			"select nickname_key from tb_user_profile where user_id in (?, ?)", String.class, first, second);
		List<String> tags = jdbcTemplate.queryForList(
			"select nickname_tag from tb_user_profile where user_id in (?, ?)", String.class, first, second);
		assertThat(keys).containsOnly("moa_kim");
		assertThat(tags).hasSize(2).doesNotHaveDuplicates().allMatch(tag -> tag.matches("\\d{4}"));
	}

	@Test
	@DisplayName("US1-AC14 그 닉네임의 태그 9999개가 모두 쓰이면 B201로 막히고, 가입 대기는 남아 다른 닉네임으로 다시 낼 수 있다")
	void exhaustedNicknameIsRejected() {
		List<Object[]> rows = new ArrayList<>();
		for (int i = 1; i <= 9999; i++) {
			rows.add(new Object[] {100_000L + i, "busyname", String.format("%04d", i)});
		}
		jdbcTemplate.batchUpdate("insert into tb_user_profile (user_id, nickname_key, nickname_tag, created_at, "
			+ "updated_at) values (?, ?, ?, now(6), now(6))", rows);
		provider.register("code-1", "google-sub-0001", "kim@example.com", true, "Kim");
		Cookie signup = callback(authorize(null), "code-1").getResponse().getCookie("oauth_signup");

		MvcTestResult exhausted = signUp(signup, "BusyName", TERMS, TERMS);

		assertThat(exhausted).hasStatus(409).bodyJson().extractingPath("$.code").isEqualTo("B201");
		assertThat(exhausted.getResponse().getCookie("oauth_signup")).isNull();
		assertThat(socialMemberCount()).isZero();
		assertThat(signUp(signup, "다른닉네임", TERMS, TERMS)).hasStatus(200);
	}

	// ---------- US2 소셜 회원이 다시 로그인한다 ----------

	@Test
	@DisplayName("US2-AC1 가입한 소셜 계정으로 다시 로그인하면 새 회원 없이 같은 회원으로 로그인된다")
	void linkedAccountLogsInAsSameMember() {
		Long userId = signUpMember("code-1", "google-sub-0001", "kim@example.com", "모아킴");
		provider.register("code-2", "google-sub-0001", "kim@example.com", true, "Kim Google");

		MvcTestResult callback = callback(authorize("/notices"), "code-2");

		assertThat(location(callback)).isEqualTo("/auth/oauth/done?next=/notices");
		Cookie refresh = callback.getResponse().getCookie("refresh_token");
		assertThat(refresh).isNotNull();
		MvcTestResult refreshed = mvc.post().uri("/api/v1/auth/token/refresh").cookie(refresh).exchange();
		assertThat(me(accessToken(refreshed))).bodyJson().extractingPath("$.id").isEqualTo(userId.intValue());
		assertThat(socialMemberCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("US2-AC2 잠긴 소셜 회원은 로그인되지 않고 잠긴 계정 안내로 간다")
	void lockedMemberIsBlocked() {
		Long userId = signUpMember("code-1", "google-sub-0001", "kim@example.com", "모아킴");
		assertThat(mvc.put().uri("/api/v1/admin/users/" + userId + "/lock")
			.header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_ID, "ADMIN")).exchange()).hasStatus2xxSuccessful();
		provider.register("code-2", "google-sub-0001", "kim@example.com", true, "Kim Google");

		MvcTestResult callback = callback(authorize(null), "code-2");

		assertThat(location(callback)).isEqualTo("/sign-in?oauthError=locked");
		assertThat(callback.getResponse().getCookie("refresh_token")).isNull();
	}

	@Test
	@DisplayName("US2-AC3 소셜 회원(비밀번호 없음)은 내 정보의 hasPassword가 거짓이라 비밀번호 변경이 보이지 않는다")
	void socialMemberHasNoPassword() {
		signUpMember("code-1", "google-sub-0001", "kim@example.com", "모아킴");
		testUsers.create(10L, "user", "user1234!", Role.USER);
		provider.register("code-2", "google-sub-0001", "kim@example.com", true, "Kim Google");
		Cookie refresh = callback(authorize(null), "code-2").getResponse().getCookie("refresh_token");
		String socialToken = accessToken(mvc.post().uri("/api/v1/auth/token/refresh").cookie(refresh).exchange());

		assertThat(me(socialToken)).bodyJson().extractingPath("$.hasPassword").isEqualTo(false);
		assertThat(mvc.get().uri(ME).header(HttpHeaders.AUTHORIZATION, bearer(10L, "USER")).exchange())
			.bodyJson().extractingPath("$.hasPassword").isEqualTo(true);
	}

	// ---------- US3 소셜 로그인이 안 될 때 안내를 받는다 ----------

	@Test
	@DisplayName("US3-AC1 제공자 화면에서 취소하면 안내 없이 로그인 화면이고 회원도 접속 이력도 생기지 않는다")
	void cancelGoesBackSilently() {
		Authorized authorized = authorize(null);

		MvcTestResult callback = mvc.get().uri(CALLBACK + "?error=access_denied&state={state}", authorized.state())
			.cookie(authorized.cookie()).exchange();

		assertThat(location(callback)).isEqualTo("/sign-in");
		assertThat(socialMemberCount()).isZero();
		assertThat(loginHistoryCount()).isZero();
	}

	@Test
	@DisplayName("US3-AC2 제공자 응답을 확인하지 못하면(state 위조, 쿠키 없음, 교환 실패) 실패 안내로 가고 로그인되지 않는다")
	void unverifiableResponseFails() {
		provider.register("code-1", "google-sub-0001", "kim@example.com", true, "Kim");
		Authorized authorized = authorize(null);

		MvcTestResult forged = mvc.get().uri(CALLBACK + "?code=code-1&state=forged").cookie(authorized.cookie())
			.exchange();
		MvcTestResult noCookie = mvc.get().uri(CALLBACK + "?code=code-1&state={state}", authorized.state()).exchange();
		MvcTestResult badCode = callback(authorize(null), "unknown-code");

		assertThat(location(forged)).isEqualTo("/sign-in?oauthError=failed");
		assertThat(location(noCookie)).isEqualTo("/sign-in?oauthError=failed");
		assertThat(location(badCode)).isEqualTo("/sign-in?oauthError=failed");
		assertThat(forged.getResponse().getCookie("refresh_token")).isNull();
		assertThat(socialMemberCount()).isZero();
	}

	@Test
	@DisplayName("US3-AC3 제공자 설정이 없으면 제공자 목록에서 빠지고 시작하면 설정 없음 안내로 간다")
	void disabledProviderIsHidden() {
		assertThat(mvc.get().uri(BASE + "/providers")).hasStatus(200).bodyJson().extractingPath("$.providers")
			.asArray().containsExactly("google");

		provider.setEnabled(false);

		assertThat(mvc.get().uri(BASE + "/providers")).bodyJson().extractingPath("$.providers").asArray().isEmpty();
		assertThat(location(mvc.get().uri(AUTHORIZE).exchange())).isEqualTo("/sign-in?oauthError=disabled");
	}

	// ---------- Edge Cases, FR-008 ----------

	@Test
	@DisplayName("Edge 두 창에서 같은 소셜 계정으로 동시에 가입하면 회원은 하나만 생기고 다른 쪽은 409다")
	void concurrentSignUpCreatesOneMember() throws Exception {
		provider.register("code-1", "google-sub-0001", "kim@example.com", true, "Kim");
		provider.register("code-2", "google-sub-0001", "kim@example.com", true, "Kim");
		Cookie first = callback(authorize(null), "code-1").getResponse().getCookie("oauth_signup");
		Cookie second = callback(authorize(null), "code-2").getResponse().getCookie("oauth_signup");
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Integer> firstStatus = executor.submit(() -> {
				start.await();
				return signUp(first, "모아킴", TERMS, TERMS).getResponse().getStatus();
			});
			Future<Integer> secondStatus = executor.submit(() -> {
				start.await();
				return signUp(second, "모아킴", TERMS, TERMS).getResponse().getStatus();
			});
			start.countDown();

			assertThat(List.of(firstStatus.get(), secondStatus.get())).containsExactlyInAnyOrder(200, 409);
		} finally {
			executor.shutdownNow();
		}
		assertThat(socialMemberCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("FR-008 소셜 로그인의 성공과 실패가 접속 이력에 남는다 (표시 방식은 US4-AC1 미정)")
	void loginHistoryRecorded() {
		Long userId = signUpMember("code-1", "google-sub-0001", "kim@example.com", "모아킴");
		provider.register("code-2", "google-sub-0002", "lee@example.com", false, "Lee");
		callback(authorize(null), "code-2");

		Integer succeeded = jdbcTemplate.queryForObject(
			"select count(*) from tb_login_history where user_id = ? and success = true", Integer.class, userId);
		Integer failed = jdbcTemplate.queryForObject("select count(*) from tb_login_history "
			+ "where login_id = 'google:google-s' and success = false and failure_code = 'A014'", Integer.class);
		assertThat(succeeded).isEqualTo(1);
		assertThat(failed).isEqualTo(1);
	}

	// ---------- 관리자 이름 바꾸기와 닉네임 태그 (research R14, tasks T060·T066) ----------

	@Test
	@DisplayName("R14 관리자가 이름을 바꿀 때 새 닉네임에서 지금 태그가 비어 있으면 태그가 그대로다")
	void adminRenameKeepsFreeTag() {
		Long userId = signUpMember("code-1", "google-sub-0001", "kim@example.com", "모아킴");
		String tag = tagOf(userId);

		assertThat(rename(userId, "새닉네임")).hasStatus(200);

		assertThat(jdbcTemplate.queryForObject("select nickname_key from tb_user_profile where user_id = ?",
			String.class, userId)).isEqualTo("새닉네임");
		assertThat(tagOf(userId)).isEqualTo(tag);
	}

	@Test
	@DisplayName("R14 관리자가 이름을 바꿀 때 새 닉네임에서 지금 태그를 다른 회원이 쓰면 새 태그를 받는다")
	void adminRenameReassignsTakenTag() {
		Long userId = signUpMember("code-1", "google-sub-0001", "kim@example.com", "모아킴");
		String tag = tagOf(userId);
		jdbcTemplate.update("insert into tb_user_profile (user_id, nickname_key, nickname_tag, created_at, updated_at) "
			+ "values (?, ?, ?, now(6), now(6))", 900L, "새닉네임", tag);

		assertThat(rename(userId, "새닉네임")).hasStatus(200);

		assertThat(tagOf(userId)).isNotEqualTo(tag).matches("\\d{4}");
	}

	@Test
	@DisplayName("R14 프로필이 있는 회원의 이름을 닉네임 규칙에 안 맞게 바꾸면 V001(칸 name)")
	void adminRenameValidatesNickname() {
		Long userId = signUpMember("code-1", "google-sub-0001", "kim@example.com", "모아킴");

		MvcTestResult result = rename(userId, "admin");

		assertThat(result).hasStatus(400).bodyJson().extractingPath("$.code").isEqualTo("V001");
		assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("name");
	}

	private MvcTestResult rename(Long userId, String name) {
		return mvc.put().uri("/api/v1/admin/users/" + userId)
			.header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_ID, "ADMIN"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + name + "\"}").exchange();
	}

	private String tagOf(Long userId) {
		return jdbcTemplate.queryForObject("select nickname_tag from tb_user_profile where user_id = ?", String.class,
			userId);
	}

	// ---------- 도우미 ----------

	private record Authorized(String state, Cookie cookie) {
	}

	private Authorized authorize(String next) {
		MvcTestResult result = next == null ? mvc.get().uri(AUTHORIZE).exchange()
			: mvc.get().uri(AUTHORIZE + "?next={next}", next).exchange();
		assertThat(result).hasStatus(302);
		String state = UriComponentsBuilder.fromUriString(location(result)).build().getQueryParams().getFirst("state");
		Cookie cookie = result.getResponse().getCookie("oauth_state");
		assertThat(cookie).isNotNull();
		return new Authorized(state, cookie);
	}

	private MvcTestResult callback(Authorized authorized, String code) {
		MvcTestResult result = mvc.get().uri(CALLBACK + "?code={code}&state={state}", code, authorized.state())
			.cookie(authorized.cookie()).exchange();
		assertThat(result).hasStatus(302);
		return result;
	}

	private MvcTestResult signUp(Cookie signup, String nickname, String serviceVersion, String privacyVersion) {
		return mvc.post().uri(SIGN_UP).cookie(signup).contentType(MediaType.APPLICATION_JSON)
			.content("{\"nickname\":\"" + nickname + "\",\"agreements\":[{\"type\":\"SERVICE\",\"version\":\""
				+ serviceVersion + "\"},{\"type\":\"PRIVACY\",\"version\":\"" + privacyVersion + "\"}]}")
			.exchange();
	}

	/** 처음 소셜 로그인부터 가입까지 하고 회원 ID를 돌려준다 */
	private Long signUpMember(String code, String subject, String email, String nickname) {
		provider.register(code, subject, email, true, "Provider Name");
		Cookie signup = callback(authorize(null), code).getResponse().getCookie("oauth_signup");
		MvcTestResult result = signUp(signup, nickname, TERMS, TERMS);
		assertThat(result).hasStatus(200);
		Number id = JsonPath.read(body(me(accessToken(result))), "$.id");
		return id.longValue();
	}

	private MvcTestResult me(String accessToken) {
		return mvc.get().uri(ME).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).exchange();
	}

	private String accessToken(MvcTestResult result) {
		return JsonPath.read(body(result), "$.accessToken");
	}

	private String location(MvcTestResult result) {
		return result.getResponse().getHeader(HttpHeaders.LOCATION);
	}

	private Integer socialMemberCount() {
		return jdbcTemplate.queryForObject("select count(*) from tb_user where login_id like 's\\_%'", Integer.class);
	}

	private Integer loginHistoryCount() {
		return jdbcTemplate.queryForObject("select count(*) from tb_login_history", Integer.class);
	}

	private String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString();
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
