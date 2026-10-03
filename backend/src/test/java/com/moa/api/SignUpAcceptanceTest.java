package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;
import com.moa.common.mail.MailMessage;
import com.moa.common.oauth.OAuthProvider;
import com.moa.common.oauth.OAuthUser;
import com.moa.common.sms.SmsMessage;
import com.moa.support.AcceptanceTest;
import com.moa.support.external.RecordingMailSender;
import com.moa.support.external.RecordingSmsSender;
import com.moa.support.external.StubOAuthClient;

import jakarta.servlet.http.Cookie;

/**
 * FR-MEM-001 회원가입. 메일·문자·OAuth는 테스트 대역(support.external)이 받는다.
 * 기존 계정은 JDBC로 직접 만들고, 가입은 API 흐름(이메일 인증 → 휴대폰 인증 → 가입 완료)으로 한다.
 */
class SignUpAcceptanceTest extends AcceptanceTest {

	private static final String EMAIL_VERIFICATIONS = "/api/v1/sign-ups/email-verifications";
	private static final String EMAIL_CONFIRMATIONS = "/api/v1/sign-ups/email-confirmations";
	private static final String PHONE_VERIFICATIONS = "/api/v1/sign-ups/phone-verifications";
	private static final String PHONE_CONFIRMATIONS = "/api/v1/sign-ups/phone-confirmations";
	private static final String MEMBERS = "/api/v1/members";
	private static final String REFRESH = "/api/v1/auth/token/refresh";

	private static final String SIGN_UP_COOKIE = "sign_up_token";
	private static final String REFRESH_COOKIE = "refresh_token";
	private static final String OAUTH_STATE_COOKIE = "oauth_state";

	private static final String EMAIL = "gamer@moa.test";
	private static final String PASSWORD = "moaPass123";
	private static final String PHONE = "01012345678";
	private static final String OTHER_PHONE = "01087654321";
	private static final LocalDate ADULT_BIRTH_DATE = LocalDate.of(2000, 1, 31);

	private static final Pattern MAIL_TOKEN = Pattern.compile("[?&]token=([^\\s&\"'<>]+)");
	private static final Pattern SMS_CODE = Pattern.compile("(?<!\\d)(\\d{6})(?!\\d)");

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private RecordingMailSender mailSender;

	@Autowired
	private RecordingSmsSender smsSender;

	@Autowired
	private StubOAuthClient oauthClient;

	@BeforeEach
	void resetExternal() {
		mailSender.clear();
		smsSender.clear();
		oauthClient.clear();
	}

	// AC-1

	@Test
	@DisplayName("FR-MEM-001 AC-1: 이메일, 비밀번호, 생년월일, 인증된 휴대폰 번호, 닉네임, 필수 동의가 모두 있으면 ACTIVE 회원이 생기고 로그인 세션이 발급된다")
	void signUpWithEmail() {
		String signUpToken = startEmailSignUp(EMAIL, PASSWORD);
		verifyPhone(signUpToken, PHONE, ADULT_BIRTH_DATE);

		MvcTestResult result = createMember(signUpToken, "moa_gamer", true, true);

		assertThat(result).hasStatus(201);
		Long memberId = memberIdByEmail(EMAIL);
		assertThat(memberCount()).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("select status from members where id = ?", String.class, memberId))
			.isEqualTo("ACTIVE");
		assertThat(jdbcTemplate.queryForObject("select nickname from members where id = ?", String.class, memberId))
			.isEqualTo("moa_gamer");
		assertThat(jdbcTemplate.queryForObject("select birth_date from members where id = ?", LocalDate.class,
			memberId)).isEqualTo(ADULT_BIRTH_DATE);
		assertThat(jdbcTemplate.queryForObject("select phone_hmac is not null and terms_agreed_at is not null "
			+ "and privacy_agreed_at is not null from members where id = ?", Boolean.class, memberId)).isTrue();
		assertThat(passwordEncoder.matches(PASSWORD, passwordHashOf(memberId))).isTrue();
		assertThat(activeLoginSessionCount(memberId)).isEqualTo(1);
		assertThat(result.getResponse().getCookie(REFRESH_COOKIE)).isNotNull();
		assertThat(currentMemberId(JsonPath.read(body(result), "$.accessToken"))).isEqualTo(memberId);
	}

	@Test
	@DisplayName("FR-MEM-001 AC-1: 구글로 가입해도 생년월일, 인증된 휴대폰 번호, 닉네임, 필수 동의가 모두 있으면 ACTIVE 회원이 생기고 로그인 세션이 발급된다")
	void signUpWithGoogle() {
		MvcTestResult callback = oauthCallback(new OAuthUser(OAuthProvider.GOOGLE, "google-1", EMAIL, true));

		assertThat(callback).hasStatus(302);
		assertThat(location(callback).getPath()).isEqualTo("/signup/phone");
		String signUpToken = cookieValue(callback, SIGN_UP_COOKIE);
		assertThat(signUpToken).isNotBlank();
		assertThat(memberCount()).isZero();

		verifyPhone(signUpToken, PHONE, ADULT_BIRTH_DATE);
		MvcTestResult result = createMember(signUpToken, "moa_gamer", true, true);

		assertThat(result).hasStatus(201);
		Long memberId = memberIdByEmail(EMAIL);
		assertThat(jdbcTemplate.queryForObject("select status from members where id = ?", String.class, memberId))
			.isEqualTo("ACTIVE");
		assertThat(socialAccountCount(memberId, OAuthProvider.GOOGLE, "google-1")).isEqualTo(1);
		assertThat(activeLoginSessionCount(memberId)).isEqualTo(1);
		assertThat(currentMemberId(JsonPath.read(body(result), "$.accessToken"))).isEqualTo(memberId);
	}

	// AC-2

	@Test
	@DisplayName("FR-MEM-001 AC-2: 이메일 인증을 마치지 않은 이메일 가입 요청은 회원을 만들지 않는다")
	void emailNotVerified() {
		assertThat(requestEmailVerification(EMAIL, PASSWORD, null)).hasStatus(201);

		assertThat(createMember(null, "moa_gamer", true, true))
			.hasStatus(401)
			.bodyJson().extractingPath("$.code").isEqualTo("A006");
		assertThat(requestPhoneCode(null, PHONE))
			.hasStatus(401)
			.bodyJson().extractingPath("$.code").isEqualTo("A006");
		assertThat(createMember("not-a-sign-up-token", "moa_gamer", true, true)).hasStatus(401);
		assertThat(memberCount()).isZero();
	}

	// AC-3

	@Test
	@DisplayName("FR-MEM-001 AC-3: 가입 나이에 미달하는 생년월일로 가입하면 거부된다")
	void underSignUpAge() {
		String signUpToken = startEmailSignUp(EMAIL, PASSWORD);
		assertThat(requestPhoneCode(signUpToken, PHONE)).hasStatus(201);

		assertThat(confirmPhone(signUpToken, lastSmsCode(PHONE), LocalDate.now().minusYears(13)))
			.hasStatus(400)
			.bodyJson().extractingPath("$.code").isEqualTo("B010");
		assertThat(createMember(signUpToken, "moa_gamer", true, true).getResponse().getStatus()).isNotEqualTo(201);
		assertThat(memberCount()).isZero();
	}

	// AC-4

	@Test
	@DisplayName("FR-MEM-001 AC-4: 이미 다른 계정에 인증된 휴대폰 번호로 가입하면 거부된다")
	void phoneAlreadyVerified() {
		signUpCompletely(EMAIL, PHONE, "first_gamer");
		ageVerifications();

		String signUpToken = startEmailSignUp("second@moa.test", PASSWORD);
		assertThat(requestPhoneCode(signUpToken, PHONE)).hasStatus(201);

		assertThat(confirmPhone(signUpToken, lastSmsCode(PHONE), ADULT_BIRTH_DATE))
			.hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("B011");
		assertThat(createMember(signUpToken, "second_gamer", true, true).getResponse().getStatus())
			.isNotEqualTo(201);
		assertThat(memberCount()).isEqualTo(1);
	}

	// AC-5

	@Test
	@DisplayName("FR-MEM-001 AC-5: 대소문자만 다른 기존 닉네임으로 가입하면 거부된다")
	void nicknameCaseInsensitive() {
		insertMember("existing@moa.test", passwordEncoder.encode("existing1"), "MoaGamer", "ACTIVE");
		String signUpToken = startEmailSignUp(EMAIL, PASSWORD);
		verifyPhone(signUpToken, PHONE, ADULT_BIRTH_DATE);

		assertThat(createMember(signUpToken, "moagamer", true, true))
			.hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("R003");
		assertThat(memberCount()).isEqualTo(1);
	}

	// AC-6

	@ParameterizedTest(name = "{0}")
	@EnumSource(OAuthProvider.class)
	@DisplayName("FR-MEM-001 AC-6: 제공자가 인증된 이메일로 알려 준 구글·카카오 계정의 이메일이 기존 계정과 같으면 새 회원을 만들지 않고 기존 계정으로 로그인된다")
	void mergeVerifiedSocialEmail(OAuthProvider provider) {
		Long existingId = insertMember(EMAIL, passwordEncoder.encode("existing1"), "existing", "ACTIVE");

		MvcTestResult callback = oauthCallback(new OAuthUser(provider, "social-1", EMAIL, true));

		assertThat(callback).hasStatus(302);
		URI location = location(callback);
		assertThat(location.getPath()).isEqualTo("/");
		assertThat(location.getQuery()).isNull();
		assertThat(memberCount()).isEqualTo(1);
		assertThat(socialAccountCount(existingId, provider, "social-1")).isEqualTo(1);
		assertThat(activeLoginSessionCount(existingId)).isEqualTo(1);
		assertThat(memberIdAfterRefresh(cookieValue(callback, REFRESH_COOKIE))).isEqualTo(existingId);
	}

	// AC-7

	@ParameterizedTest(name = "{0}")
	@EnumSource(OAuthProvider.class)
	@DisplayName("FR-MEM-001 AC-7: 제공자가 인증되지 않은 이메일로 알려 준 경우에는 기존 계정에 합치지 않는다")
	void doNotMergeUnverifiedSocialEmail(OAuthProvider provider) {
		Long existingId = insertMember(EMAIL, passwordEncoder.encode("existing1"), "existing", "ACTIVE");

		MvcTestResult callback = oauthCallback(new OAuthUser(provider, "social-1", EMAIL, false));

		assertThat(callback).hasStatus(302);
		assertThat(location(callback).getQuery()).contains("error=B003");
		assertThat(totalSocialAccountCount()).isZero();
		assertThat(activeLoginSessionCount(existingId)).isZero();
		assertThat(cookieValue(callback, REFRESH_COOKIE)).isNull();
		assertThat(memberCount()).isEqualTo(1);
	}

	// AC-8

	@Test
	@DisplayName("FR-MEM-001 AC-8: 비밀번호가 없는 기존 계정의 이메일로 이메일 가입을 하면, "
		+ "이메일 인증 뒤 새 회원이 생기지 않고 기존 계정에 비밀번호가 추가되어 그 이메일·비밀번호로 로그인된다")
	void addPasswordToExistingAccount() {
		Long existingId = insertMember(EMAIL, null, "existing", "ACTIVE");
		insertSocialAccount(existingId, OAuthProvider.GOOGLE, "google-1");

		assertThat(requestEmailVerification(EMAIL, PASSWORD, null)).hasStatus(201);
		assertThat(memberCount()).isEqualTo(1);
		assertThat(passwordHashOf(existingId)).isNull();

		MvcTestResult confirmation = confirmEmail(lastMailToken(EMAIL));

		assertThat(confirmation).hasStatus(200);
		assertThat(confirmation).bodyJson().extractingPath("$.result").isEqualTo("SIGNED_IN");
		assertThat(memberCount()).isEqualTo(1);
		assertThat(passwordEncoder.matches(PASSWORD, passwordHashOf(existingId))).isTrue();
		assertThat(activeLoginSessionCount(existingId)).isEqualTo(1);
		assertThat(currentMemberId(JsonPath.read(body(confirmation), "$.accessToken"))).isEqualTo(existingId);
	}

	// AC-9

	@Test
	@DisplayName("FR-MEM-001 AC-9: 비밀번호가 있는 기존 계정의 이메일로 이메일 가입을 하면 거부된다")
	void rejectEmailOfAccountWithPassword() {
		String existingHash = passwordEncoder.encode("existing1");
		Long existingId = insertMember(EMAIL, existingHash, "existing", "ACTIVE");

		assertThat(requestEmailVerification(EMAIL, PASSWORD, null))
			.hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("B002");
		assertThat(mailSender.sent()).noneMatch(message -> EMAIL.equals(message.to()));
		assertThat(memberCount()).isEqualTo(1);
		assertThat(passwordHashOf(existingId)).isEqualTo(existingHash);
	}

	// AC-10

	@Test
	@DisplayName("FR-MEM-001 AC-10: 카카오가 이메일을 주지 않으면 이메일 인증을 마치기 전에는 회원이 생기지 않는다")
	void kakaoWithoutEmailNeedsEmailVerification() {
		MvcTestResult callback = oauthCallback(new OAuthUser(OAuthProvider.KAKAO, "kakao-1", null, false));

		assertThat(callback).hasStatus(302);
		assertThat(location(callback).getPath()).isEqualTo("/signup/email");
		String signUpToken = cookieValue(callback, SIGN_UP_COOKIE);
		assertThat(signUpToken).isNotBlank();

		assertThat(requestPhoneCode(signUpToken, PHONE))
			.hasStatus(409)
			.bodyJson().extractingPath("$.code").isEqualTo("B015");
		assertThat(createMember(signUpToken, "moa_gamer", true, true).getResponse().getStatus()).isNotEqualTo(201);
		assertThat(requestEmailVerification(EMAIL, PASSWORD, signUpToken)).hasStatus(201);
		assertThat(createMember(signUpToken, "moa_gamer", true, true).getResponse().getStatus()).isNotEqualTo(201);
		assertThat(memberCount()).isZero();
		assertThat(totalSocialAccountCount()).isZero();
	}

	@Test
	@DisplayName("FR-MEM-001 AC-10: 카카오가 이메일을 주지 않았고 인증한 이메일이 기존 계정 이메일이면 그 계정에 카카오가 연결된다")
	void kakaoWithoutEmailLinksToExistingAccount() {
		Long existingId = insertMember(EMAIL, null, "existing", "ACTIVE");
		insertSocialAccount(existingId, OAuthProvider.GOOGLE, "google-1");
		String signUpToken = cookieValue(
			oauthCallback(new OAuthUser(OAuthProvider.KAKAO, "kakao-1", null, false)), SIGN_UP_COOKIE);

		assertThat(requestEmailVerification(EMAIL, PASSWORD, signUpToken)).hasStatus(201);
		MvcTestResult confirmation = confirmEmail(lastMailToken(EMAIL));

		assertThat(confirmation).hasStatus(200);
		assertThat(confirmation).bodyJson().extractingPath("$.result").isEqualTo("SIGNED_IN");
		assertThat(memberCount()).isEqualTo(1);
		assertThat(socialAccountCount(existingId, OAuthProvider.KAKAO, "kakao-1")).isEqualTo(1);
		assertThat(currentMemberId(JsonPath.read(body(confirmation), "$.accessToken"))).isEqualTo(existingId);
	}

	// AC-13

	@ParameterizedTest(name = "{0}")
	@EnumSource(OAuthProvider.class)
	@DisplayName("FR-MEM-001 AC-13: 구글·카카오 계정을 기존 계정에 합치면 그 계정의 알림 목록에 로그인 수단 연결 알림이 생긴다")
	void notifyOnMerge(OAuthProvider provider) {
		Long existingId = insertMember(EMAIL, passwordEncoder.encode("existing1"), "existing", "ACTIVE");

		assertThat(oauthCallback(new OAuthUser(provider, "social-1", EMAIL, true))).hasStatus(302);

		Long socialAccountId = jdbcTemplate.queryForObject(
			"select id from member_social_accounts where member_id = ? and provider = ?", Long.class, existingId,
			provider.name());
		assertLinkedNotification(existingId, socialAccountId);
	}

	@Test
	@DisplayName("FR-MEM-001 AC-13: 이메일을 주지 않은 카카오 계정을 이메일 인증으로 기존 계정에 연결해도 로그인 수단 연결 알림이 생긴다")
	void notifyOnKakaoLinkByEmailVerification() {
		Long existingId = insertMember(EMAIL, null, "existing", "ACTIVE");
		insertSocialAccount(existingId, OAuthProvider.GOOGLE, "google-1");
		String signUpToken = cookieValue(
			oauthCallback(new OAuthUser(OAuthProvider.KAKAO, "kakao-1", null, false)), SIGN_UP_COOKIE);
		assertThat(requestEmailVerification(EMAIL, PASSWORD, signUpToken)).hasStatus(201);

		assertThat(confirmEmail(lastMailToken(EMAIL))).hasStatus(200);

		Long kakaoAccountId = jdbcTemplate.queryForObject(
			"select id from member_social_accounts where member_id = ? and provider = 'KAKAO'", Long.class,
			existingId);
		assertLinkedNotification(existingId, kakaoAccountId);
	}

	// AC-14

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = MemberStatusValue.class)
	@DisplayName("FR-MEM-001 AC-14: 같은 이메일의 기존 계정이 ACTIVE가 아니면 합치지 않고 새 회원도 만들지 않는다")
	void doNotMergeIntoInactiveAccount(MemberStatusValue status) {
		Long existingId = insertMember(EMAIL, passwordEncoder.encode("existing1"), "existing", status.name());

		MvcTestResult callback = oauthCallback(new OAuthUser(OAuthProvider.GOOGLE, "google-1", EMAIL, true));

		assertThat(callback).hasStatus(302);
		assertThat(location(callback).getQuery()).contains("error=B003");
		assertThat(totalSocialAccountCount()).isZero();
		assertThat(activeLoginSessionCount(existingId)).isZero();
		assertThat(cookieValue(callback, REFRESH_COOKIE)).isNull();
		assertThat(memberCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("FR-MEM-001 AC-14: 같은 이메일의 기존 계정에 같은 제공자의 다른 계정이 이미 연결돼 있으면 합치지 않고 새 회원도 만들지 않는다")
	void doNotMergeWhenSameProviderAlreadyLinked() {
		Long existingId = insertMember(EMAIL, passwordEncoder.encode("existing1"), "existing", "ACTIVE");
		insertSocialAccount(existingId, OAuthProvider.GOOGLE, "google-old");

		MvcTestResult callback = oauthCallback(new OAuthUser(OAuthProvider.GOOGLE, "google-new", EMAIL, true));

		assertThat(callback).hasStatus(302);
		assertThat(location(callback).getQuery()).contains("error=B003");
		assertThat(totalSocialAccountCount()).isEqualTo(1);
		assertThat(socialAccountCount(existingId, OAuthProvider.GOOGLE, "google-old")).isEqualTo(1);
		assertThat(activeLoginSessionCount(existingId)).isZero();
		assertThat(cookieValue(callback, REFRESH_COOKIE)).isNull();
		assertThat(memberCount()).isEqualTo(1);
	}

	/** AC-14의 ACTIVE가 아닌 계정 상태 (PURGED는 이메일이 지워져 같은 이메일 계정이 될 수 없다) */
	enum MemberStatusValue {
		SUSPENDED,
		WITHDRAWN
	}

	// 가입 흐름

	/** 이메일 가입을 시작하고 인증 링크를 열어 가입 진행 토큰을 받는다 */
	private String startEmailSignUp(String email, String password) {
		assertThat(requestEmailVerification(email, password, null)).hasStatus(201);
		MvcTestResult confirmation = confirmEmail(lastMailToken(email));
		assertThat(confirmation).hasStatus(200);
		assertThat(confirmation).bodyJson().extractingPath("$.result").isEqualTo("CONTINUE_SIGN_UP");
		String signUpToken = cookieValue(confirmation, SIGN_UP_COOKIE);
		assertThat(signUpToken).isNotBlank();
		return signUpToken;
	}

	private void verifyPhone(String signUpToken, String phone, LocalDate birthDate) {
		assertThat(requestPhoneCode(signUpToken, phone)).hasStatus(201);
		assertThat(confirmPhone(signUpToken, lastSmsCode(phone), birthDate)).hasStatus(204);
	}

	private void signUpCompletely(String email, String phone, String nickname) {
		String signUpToken = startEmailSignUp(email, PASSWORD);
		verifyPhone(signUpToken, phone, ADULT_BIRTH_DATE);
		assertThat(createMember(signUpToken, nickname, true, true)).hasStatus(201);
	}

	private MvcTestResult requestEmailVerification(String email, String password, String signUpToken) {
		var request = mvc.post().uri(EMAIL_VERIFICATIONS)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
		if (signUpToken != null) {
			request = request.cookie(new Cookie(SIGN_UP_COOKIE, signUpToken));
		}
		return request.exchange();
	}

	private MvcTestResult confirmEmail(String token) {
		return mvc.post().uri(EMAIL_CONFIRMATIONS)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"token\":\"" + token + "\"}")
			.exchange();
	}

	private MvcTestResult requestPhoneCode(String signUpToken, String phone) {
		var request = mvc.post().uri(PHONE_VERIFICATIONS)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"phoneNumber\":\"" + phone + "\"}");
		if (signUpToken != null) {
			request = request.cookie(new Cookie(SIGN_UP_COOKIE, signUpToken));
		}
		return request.exchange();
	}

	private MvcTestResult confirmPhone(String signUpToken, String code, LocalDate birthDate) {
		return mvc.post().uri(PHONE_CONFIRMATIONS)
			.cookie(new Cookie(SIGN_UP_COOKIE, signUpToken))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"verificationCode\":\"" + code + "\",\"birthDate\":\"" + birthDate + "\"}")
			.exchange();
	}

	private MvcTestResult createMember(String signUpToken, String nickname, boolean termsAgreed,
		boolean privacyAgreed) {
		var request = mvc.post().uri(MEMBERS)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"nickname\":\"" + nickname + "\",\"termsAgreed\":" + termsAgreed + ",\"privacyAgreed\":"
				+ privacyAgreed + "}");
		if (signUpToken != null) {
			request = request.cookie(new Cookie(SIGN_UP_COOKIE, signUpToken));
		}
		return request.exchange();
	}

	/** 가입 화면에서 시작해 제공자 동의를 마치고 콜백으로 돌아온다. 제공자는 user를 알려 준다 */
	private MvcTestResult oauthCallback(OAuthUser user) {
		String provider = user.provider().pathValue();
		MvcTestResult authorize = mvc.get().uri("/api/v1/auth/oauth/{provider}?intent=SIGN_UP", provider)
			.exchange();
		assertThat(authorize).hasStatus(302);
		String state = cookieValue(authorize, OAUTH_STATE_COOKIE);
		assertThat(state).isNotBlank();

		String code = "code-" + UUID.randomUUID();
		oauthClient.willReturn(code, user);
		return mvc.get().uri("/api/v1/auth/oauth/{provider}/callback?code={code}&state={state}", provider, code, state)
			.cookie(new Cookie(OAUTH_STATE_COOKIE, state))
			.exchange();
	}

	/** 시험 중 다시 보내기 간격(60초)에 걸리지 않도록 앞서 보낸 인증의 발송 시각을 2분 앞당긴다 */
	private void ageVerifications() {
		jdbcTemplate.update("update phone_verifications set created_at = created_at - interval 2 minute");
		jdbcTemplate.update("update email_verifications set created_at = created_at - interval 2 minute");
	}

	// 테스트 대역에서 읽기

	private String lastMailToken(String email) {
		MailMessage message = mailSender.sent().stream()
			.filter(sent -> email.equals(sent.to()))
			.reduce((first, second) -> second)
			.orElseThrow(() -> new AssertionError(email + "로 보낸 인증 메일이 없다"));
		Matcher matcher = MAIL_TOKEN.matcher(message.body());
		assertThat(matcher.find()).as("인증 메일 본문의 링크 token").isTrue();
		return URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
	}

	private String lastSmsCode(String phone) {
		SmsMessage message = smsSender.sent().stream()
			.filter(sent -> phone.equals(sent.to()))
			.reduce((first, second) -> second)
			.orElseThrow(() -> new AssertionError(phone + "로 보낸 인증 문자가 없다"));
		Matcher matcher = SMS_CODE.matcher(message.text());
		assertThat(matcher.find()).as("인증 문자 본문의 6자리 인증 번호").isTrue();
		return matcher.group(1);
	}

	// 로그인 확인

	/** 액세스 토큰으로 인증된 요청을 보내 누구로 로그인됐는지 읽는다 */
	private Long currentMemberId(String accessToken) {
		MvcTestResult me = mvc.get().uri("/test/items/me")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.exchange();
		assertThat(me).hasStatus(200);
		return Long.valueOf(body(me));
	}

	/** 리프레시 쿠키로 재발급받아 누구로 로그인됐는지 읽는다 */
	private Long memberIdAfterRefresh(String refreshToken) {
		assertThat(refreshToken).isNotBlank();
		MvcTestResult refresh = mvc.post().uri(REFRESH).cookie(new Cookie(REFRESH_COOKIE, refreshToken)).exchange();
		assertThat(refresh).hasStatus(200);
		return currentMemberId(JsonPath.read(body(refresh), "$.accessToken"));
	}

	// 데이터

	private Long insertMember(String email, String passwordHash, String nickname, String status) {
		jdbcTemplate.update("insert into members (email, password_hash, nickname, birth_date, role, status, "
			+ "terms_agreed_at, privacy_agreed_at, created_at, created_by, updated_at, updated_by) "
			+ "values (?, ?, ?, ?, 'MEMBER', ?, now(6), now(6), now(6), 1, now(6), 1)",
			email, passwordHash, nickname, ADULT_BIRTH_DATE, status);
		return memberIdByEmail(email);
	}

	private void insertSocialAccount(Long memberId, OAuthProvider provider, String providerUserId) {
		jdbcTemplate.update("insert into member_social_accounts (member_id, provider, provider_user_id, created_at, "
			+ "created_by, updated_at, updated_by) values (?, ?, ?, now(6), 1, now(6), 1)",
			memberId, provider.name(), providerUserId);
	}

	private void assertLinkedNotification(Long memberId, Long socialAccountId) {
		assertThat(jdbcTemplate.queryForObject("select count(*) from notifications where member_id = ? "
			+ "and type = 'LOGIN_METHOD_LINKED' and target_type = 'MEMBER_SOCIAL_ACCOUNT' and target_id = ?",
			Integer.class, memberId, socialAccountId)).isEqualTo(1);
	}

	private Long memberIdByEmail(String email) {
		return jdbcTemplate.queryForObject("select id from members where email = ? and deleted_at is null",
			Long.class, email);
	}

	private String passwordHashOf(Long memberId) {
		return jdbcTemplate.queryForObject("select password_hash from members where id = ?", String.class,
			memberId);
	}

	private int memberCount() {
		return jdbcTemplate.queryForObject("select count(*) from members", Integer.class);
	}

	private int socialAccountCount(Long memberId, OAuthProvider provider, String providerUserId) {
		return jdbcTemplate.queryForObject("select count(*) from member_social_accounts where member_id = ? "
			+ "and provider = ? and provider_user_id = ? and deleted_at is null",
			Integer.class, memberId, provider.name(), providerUserId);
	}

	private int totalSocialAccountCount() {
		return jdbcTemplate.queryForObject("select count(*) from member_social_accounts", Integer.class);
	}

	private int activeLoginSessionCount(Long memberId) {
		return jdbcTemplate.queryForObject(
			"select count(*) from login_sessions where member_id = ? and revoked_at is null", Integer.class,
			memberId);
	}

	// 응답

	private static URI location(MvcTestResult result) {
		String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
		assertThat(location).isNotBlank();
		return URI.create(location);
	}

	/** Set-Cookie로 받은 쿠키 값. 없거나 지우는 쿠키(빈 값)면 null */
	private static String cookieValue(MvcTestResult result, String name) {
		Cookie cookie = result.getResponse().getCookie(name);
		if (cookie == null || cookie.getValue() == null || cookie.getValue().isEmpty()) {
			return null;
		}
		return cookie.getValue();
	}

	private static String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString();
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
