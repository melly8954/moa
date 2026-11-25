package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;
import com.moa.common.security.Role;
import com.moa.support.AcceptanceTest;
import com.moa.support.CodeSeed;
import com.moa.support.DatabaseCleaner;
import com.moa.support.TestUsers;

/**
 * 마이페이지 (specs/009-mypage US1~US4·US6). 게임 정보(US5)는 008 공통코드 시드가 들어온 뒤 더한다.
 * 화면에서만 보이는 AC(US1-AC2·AC3·AC5·AC8)는 프론트 단위 테스트와 e2e가 본다.
 * 태그 충돌 다시 하기는 MyPageRetryAcceptanceTest가 본다.
 */
class MyPageAcceptanceTest extends AcceptanceTest {

	private static final String ME = "/api/v1/users/me";
	private static final String PROFILE = "/api/v1/users/me/profile";
	private static final String NICKNAME = "/api/v1/users/me/nickname";
	private static final String AVATARS = "/api/v1/profile/avatars";
	private static final long MEMBER_ID = 10L;
	private static final long OTHER_ID = 11L;
	private static final String EMPTY_PROFILE = """
		{"avatar":null,"bio":null,"birthYear":null,"gender":null,"ageGenderPublic":false,"discordTag":null,
		"discordVisibility":null,"playTimes":[]}""";
	private static final String FULL_PROFILE = """
		{"avatar":"cat","bio":"같이 해요","birthYear":2000,"gender":"FEMALE","ageGenderPublic":true,
		"discordTag":"moa_user.01","discordVisibility":"MEMBERS","playTimes":["WEEKEND_ALL_DAY","WEEKDAY_EVENING"]}""";

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private CodeSeed codeSeed;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DatabaseCleaner databaseCleaner;

	private String member;

	/** 로컬 계정 SQL의 admin·user가 남아 있을 수 있어 비우고 시작한다 */
	@BeforeEach
	void setUp() {
		databaseCleaner.clean();
		codeSeed.restore();
		testUsers.create(1L, "system", null, Role.USER);
		testUsers.create(MEMBER_ID, "moa_kim", null, Role.USER);
		member = bearer(MEMBER_ID, "USER");
	}

	// ---------- US1 내 프로필을 본다 ----------

	@Test
	@DisplayName("US1-AC1 내 프로필에 아바타·닉네임·태그·소개·나이·성별·디스코드·시간대가 보이고, 적지 않은 항목은 비어 있다")
	void myProfile() {
		profileRow(MEMBER_ID, "moa_kim", "0421");

		MvcTestResult empty = get(PROFILE, member);

		assertThat(empty).hasStatus(200);
		assertThat(empty).bodyJson().extractingPath("$.nickname").isEqualTo("moa_kim");
		assertThat(empty).bodyJson().extractingPath("$.nicknameTag").isEqualTo("0421");
		for (String field : List.of("bio", "birthYear", "age", "gender", "discordTag", "nicknameChangeableAt")) {
			assertThat(empty).bodyJson().extractingPath("$." + field).isNull();
		}
		assertThat(empty).bodyJson().extractingPath("$.playTimes").asArray().isEmpty();

		assertThat(save(FULL_PROFILE)).hasStatus(200);
		MvcTestResult full = get(PROFILE, member);

		assertThat(full).bodyJson().extractingPath("$.avatar").isEqualTo("cat");
		assertThat(full).bodyJson().extractingPath("$.bio").isEqualTo("같이 해요");
		assertThat(full).bodyJson().extractingPath("$.birthYear").isEqualTo(2000);
		assertThat(full).bodyJson().extractingPath("$.age").isEqualTo(Year.now().getValue() - 2000);
		assertThat(full).bodyJson().extractingPath("$.gender").isEqualTo("FEMALE");
		assertThat(full).bodyJson().extractingPath("$.ageGenderPublic").isEqualTo(true);
		assertThat(full).bodyJson().extractingPath("$.discordTag").isEqualTo("moa_user.01");
		assertThat(full).bodyJson().extractingPath("$.discordVisibility").isEqualTo("MEMBERS");
		assertThat(full).bodyJson().extractingPath("$.playTimes[*].name").asArray()
			.containsExactly("평일 저녁", "주말 종일");
	}

	@Test
	@DisplayName("US1-AC4 아바타를 고른 적이 없으면 내 프로필과 내 정보의 아바타가 default다")
	void defaultAvatar() {
		assertThat(get(PROFILE, member)).bodyJson().extractingPath("$.avatar").isEqualTo("default");
		assertThat(get(ME, member)).bodyJson().extractingPath("$.avatar").isEqualTo("default");

		profileRow(MEMBER_ID, "moa_kim", "0421");

		assertThat(get(PROFILE, member)).bodyJson().extractingPath("$.avatar").isEqualTo("default");
		assertThat(get(ME, member)).bodyJson().extractingPath("$.avatar").isEqualTo("default");
	}

	@Test
	@DisplayName("US1-AC6 내 정보의 mypageAllowed는 회원이면 참, 관리자·운영자 계정이면 거짓이다")
	void mypageAllowed() {
		assertThat(get(ME, member)).bodyJson().extractingPath("$.mypageAllowed").isEqualTo(true);
		assertThat(get(ME, superAdmin())).bodyJson().extractingPath("$.mypageAllowed").isEqualTo(false);
		assertThat(get(ME, operator(30L, "op", "CODE_MANAGE"))).bodyJson().extractingPath("$.mypageAllowed")
			.isEqualTo(false);
	}

	@Test
	@DisplayName("US1-AC7 관리자·운영자 계정은 마이페이지 API가 모두 A201이고 아무것도 바뀌지 않는다. 아바타 목록은 열린다")
	void managersAreBlocked() {
		for (String manager : List.of(superAdmin(), operator(30L, "op", "CODE_MANAGE"))) {
			assertCode(get(PROFILE, manager), 403, "A201");
			assertCode(put(PROFILE, manager, FULL_PROFILE), 403, "A201");
			assertCode(put(NICKNAME, manager, "{\"nickname\":\"새이름\"}"), 403, "A201");
			assertThat(get(AVATARS, manager)).hasStatus(200).bodyJson().extractingPath("$.avatars").asArray()
				.contains("default");
		}
		assertThat(count("select count(*) from tb_user_profile")).isZero();
		assertThat(count("select count(*) from tb_user where name = '새이름'")).isZero();
	}

	// ---------- US2 닉네임을 바꾼다 ----------

	@Test
	@DisplayName("US2-AC1 닉네임을 바꾸면 저장되고 내 프로필과 내 정보(계정 바)에 새 닉네임이 보인다")
	void changeNickname() {
		profileRow(MEMBER_ID, "moa_kim", "0421");

		MvcTestResult changed = changeNickname("  새닉네임  ");

		assertThat(changed).hasStatus(200);
		assertThat(changed).bodyJson().extractingPath("$.nickname").isEqualTo("새닉네임");
		assertThat(get(PROFILE, member)).bodyJson().extractingPath("$.nickname").isEqualTo("새닉네임");
		assertThat(get(ME, member)).bodyJson().extractingPath("$.name").isEqualTo("새닉네임");
	}

	@Test
	@DisplayName("US2-AC2 바꾼 닉네임은 다른 기능이 쓰는 표시 이름(tb_user.name)에 반영된다 (파티 멤버 목록은 008 구현 뒤)")
	void nicknameIsDisplayName() {
		profileRow(MEMBER_ID, "moa_kim", "0421");

		assertThat(changeNickname("새닉네임")).hasStatus(200);

		assertThat(text("select name from tb_user where id = ?", MEMBER_ID)).isEqualTo("새닉네임");
		assertThat(text("select nickname_key from tb_user_profile where user_id = ?", MEMBER_ID)).isEqualTo("새닉네임");
	}

	@ParameterizedTest(name = "US2-AC3 [{0}]")
	@ValueSource(strings = {"", "   ", "가", "가나다라마바사아자차카타파", "모아!", "모 아", "admin", "MOA", "운영자"})
	@DisplayName("US2-AC3 비었거나 규칙(1자·13자·글자·예약어)에 안 맞는 닉네임은 저장되지 않고 V001(칸 nickname)이다")
	void invalidNickname(String nickname) {
		profileRow(MEMBER_ID, "moa_kim", "0421");

		MvcTestResult result = changeNickname(nickname);

		assertFieldError(result, "nickname");
		assertThat(text("select name from tb_user where id = ?", MEMBER_ID)).isEqualTo("moa_kim");
		assertThat(nicknameChangedAt()).isNull();
	}

	@Test
	@DisplayName("US2-AC4 바꾼 지 30일이 안 됐으면 B202이고, 내 프로필에 다시 바꿀 수 있는 시각이 보인다. 30일이 지나면 바꿀 수 있다")
	void nicknameCooldown() throws Exception {
		profileRow(MEMBER_ID, "moa_kim", "0421");
		assertThat(changeNickname("첫이름")).hasStatus(200);

		assertCode(changeNickname("둘째이름"), 409, "B202");

		assertThat(text("select name from tb_user where id = ?", MEMBER_ID)).isEqualTo("첫이름");
		String changeableAt = JsonPath.read(body(get(PROFILE, member)), "$.nicknameChangeableAt");
		assertThat(OffsetDateTime.parse(changeableAt).toInstant())
			.isCloseTo(OffsetDateTime.now().plusDays(30).toInstant(), within(5, ChronoUnit.MINUTES));

		nicknameChangedAt(LocalDateTime.now().minusDays(30).plusHours(1));
		assertCode(changeNickname("둘째이름"), 409, "B202");
		nicknameChangedAt(LocalDateTime.now().minusDays(30).minusMinutes(1));
		assertThat(get(PROFILE, member)).bodyJson().extractingPath("$.nicknameChangeableAt").isNull();
		assertThat(changeNickname("둘째이름")).hasStatus(200);
	}

	@Test
	@DisplayName("US2-AC5 다른 회원이 쓰는 닉네임(대소문자만 다른 것 포함)으로도 바뀌고 서로 다른 태그를 갖는다")
	void sameNicknameGetsDifferentTag() {
		testUsers.create(OTHER_ID, "Moa_Lee", null, Role.USER);
		profileRow(OTHER_ID, "moa_lee", "0421");
		testUsers.create(12L, "third", null, Role.USER);
		profileRow(12L, "third", "0421");
		profileRow(MEMBER_ID, "moa_kim", "0421");

		assertThat(changeNickname("Moa_Lee")).hasStatus(200);
		assertThat(put(NICKNAME, bearer(12L, "USER"), "{\"nickname\":\"MOA_LEE\"}")).hasStatus(200);

		assertThat(jdbcTemplate.queryForList("select nickname_tag from tb_user_profile where nickname_key = 'moa_lee'",
			String.class)).hasSize(3).doesNotHaveDuplicates().allMatch(tag -> tag.matches("\\d{4}"));
	}

	@Test
	@DisplayName("US2-AC6 가입 뒤 첫 닉네임 변경은 기다리지 않고 바로 된다")
	void firstChangeIsImmediate() {
		profileRow(MEMBER_ID, "moa_kim", "0421");
		assertThat(get(PROFILE, member)).bodyJson().extractingPath("$.nicknameChangeableAt").isNull();

		assertThat(changeNickname("첫이름")).hasStatus(200);

		assertThat(nicknameChangedAt()).isNotNull();
	}

	@Test
	@DisplayName("US2-AC7 새 닉네임에서 지금 태그가 비어 있으면 태그가 그대로고, 다른 회원이 쓰면 새 태그를 받는다")
	void tagIsKeptOrReassigned() {
		profileRow(MEMBER_ID, "moa_kim", "0421");

		assertThat(changeNickname("빈이름")).bodyJson().extractingPath("$.nicknameTag").isEqualTo("0421");

		nicknameChangedAt(null);
		testUsers.create(OTHER_ID, "쓰는이름", null, Role.USER);
		profileRow(OTHER_ID, "쓰는이름", "0421");
		assertThat(changeNickname("쓰는이름")).bodyJson().extractingPath("$.nicknameTag").asString()
			.matches("\\d{4}")
			.isNotEqualTo("0421");
	}

	@Test
	@DisplayName("US2-AC8 그 닉네임(대소문자 무시)의 태그 9999개가 모두 쓰이면 B201이고 닉네임이 바뀌지 않는다")
	void exhaustedNickname() {
		profileRow(MEMBER_ID, "moa_kim", "0421");
		List<Object[]> rows = new ArrayList<>();
		for (int i = 1; i <= 9999; i++) {
			rows.add(new Object[] {100_000L + i, "busyname", String.format("%04d", i)});
		}
		jdbcTemplate.batchUpdate("insert into tb_user_profile (user_id, nickname_key, nickname_tag, created_at, "
			+ "updated_at) values (?, ?, ?, now(6), now(6))", rows);

		assertCode(changeNickname("BusyName"), 409, "B201");

		assertThat(text("select name from tb_user where id = ?", MEMBER_ID)).isEqualTo("moa_kim");
		assertThat(nicknameChangedAt()).isNull();
	}

	// ---------- US3 자기소개, 나이·성별, 디스코드 태그 ----------

	@Test
	@DisplayName("US3-AC1 자기소개는 500자까지 저장되고, 넘으면 저장되지 않고 V001(칸 bio)이다")
	void bio() {
		assertThat(save(profile("\"bio\":\"" + "가".repeat(500) + "\""))).hasStatus(200);
		assertThat(get(PROFILE, member)).bodyJson().extractingPath("$.bio").isEqualTo("가".repeat(500));

		assertFieldError(save(profile("\"bio\":\"" + "나".repeat(501) + "\"")), "bio");
		assertThat(get(PROFILE, member)).bodyJson().extractingPath("$.bio").isEqualTo("가".repeat(500));
	}

	@Test
	@DisplayName("US3-AC2 출생 연도·성별·공개 여부 하나가 저장되고 나이로 보인다. 미래·1900년 전 연도와 목록 밖 성별은 V001이다")
	void ageAndGender() {
		int thisYear = Year.now().getValue();

		MvcTestResult saved = save(profile("\"birthYear\":" + (thisYear - 20)
			+ ",\"gender\":\"MALE\",\"ageGenderPublic\":true"));

		assertThat(saved).hasStatus(200);
		assertThat(saved).bodyJson().extractingPath("$.age").isEqualTo(20);
		assertThat(saved).bodyJson().extractingPath("$.gender").isEqualTo("MALE");
		assertThat(saved).bodyJson().extractingPath("$.ageGenderPublic").isEqualTo(true);
		assertThat(save(profile("\"birthYear\":" + thisYear + ",\"ageGenderPublic\":false"))).bodyJson()
			.extractingPath("$.ageGenderPublic").isEqualTo(false);

		assertFieldError(save(profile("\"birthYear\":" + (thisYear + 1))), "birthYear");
		assertFieldError(save(profile("\"birthYear\":1899")), "birthYear");
		assertFieldError(save(profile("\"gender\":\"OTHER\"")), "gender");
		assertThat(get(PROFILE, member)).bodyJson().extractingPath("$.birthYear").isEqualTo(thisYear);
	}

	@Test
	@DisplayName("US3-AC3 디스코드 태그와 공개 범위가 저장되고, 공개 범위를 보내지 않으면 비공개다. 목록 밖 공개 범위는 V001이다")
	void discord() {
		for (String visibility : List.of("FRIENDS", "MEMBERS", "PRIVATE")) {
			MvcTestResult saved = save(
				profile("\"discordTag\":\"moa_user.01\",\"discordVisibility\":\"" + visibility + "\""));
			assertThat(saved).hasStatus(200);
			assertThat(saved).bodyJson().extractingPath("$.discordTag").isEqualTo("moa_user.01");
			assertThat(saved).bodyJson().extractingPath("$.discordVisibility").isEqualTo(visibility);
		}

		assertThat(save(profile("\"discordTag\":\"moa_user.01\",\"discordVisibility\":\"MEMBERS\""))).hasStatus(200);
		assertThat(put(PROFILE, member, "{\"discordTag\":\"moa_user.01\"}")).bodyJson()
			.extractingPath("$.discordVisibility").isEqualTo("PRIVATE");
		assertFieldError(save(profile("\"discordVisibility\":\"EVERYONE\"")), "discordVisibility");
	}

	@ParameterizedTest(name = "US3-AC4 [{0}]")
	@ValueSource(strings = {"name#1234", "Moa_User", "a", "moa user", "moa..user", "모아유저",
		"a23456789012345678901234567890123"})
	@DisplayName("US3-AC4 디스코드 사용자명 형식에 맞지 않으면 저장되지 않고 V001(칸 discordTag)이다")
	void invalidDiscordTag(String tag) {
		assertFieldError(save(profile("\"discordTag\":\"" + tag + "\"")), "discordTag");

		assertThat(get(PROFILE, member)).bodyJson().extractingPath("$.discordTag").isNull();
	}

	@Test
	@DisplayName("US3-AC5 적어 둔 값을 지우고 저장하면 비어 있음으로 저장된다")
	void clearValues() {
		assertThat(save(FULL_PROFILE)).hasStatus(200);

		MvcTestResult cleared = save(EMPTY_PROFILE);

		assertThat(cleared).hasStatus(200);
		for (String field : List.of("bio", "birthYear", "age", "gender", "discordTag")) {
			assertThat(cleared).bodyJson().extractingPath("$." + field).isNull();
		}
		assertThat(cleared).bodyJson().extractingPath("$.discordVisibility").isEqualTo("PRIVATE");
	}

	// ---------- US4 기본 아바타를 고른다 ----------

	@Test
	@DisplayName("US4-AC1 목록에서 아바타를 골라 저장하면 내 프로필과 내 정보(계정 바)에 보인다")
	void chooseAvatar() {
		assertThat(get(AVATARS, member)).hasStatus(200).bodyJson().extractingPath("$.avatars").asArray()
			.containsExactly("default", "cat", "dog", "fox", "bear", "rabbit", "penguin", "robot");

		assertThat(save(profile("\"avatar\":\"fox\""))).hasStatus(200);

		assertThat(get(PROFILE, member)).bodyJson().extractingPath("$.avatar").isEqualTo("fox");
		assertThat(get(ME, member)).bodyJson().extractingPath("$.avatar").isEqualTo("fox");
	}

	@Test
	@DisplayName("US4-AC2 목록 밖의 아바타 값은 저장되지 않고 V001(칸 avatar)이다")
	void avatarOutsideList() {
		assertThat(save(profile("\"avatar\":\"fox\""))).hasStatus(200);

		assertFieldError(save(profile("\"avatar\":\"dragon\"")), "avatar");
		assertFieldError(save(profile("\"avatar\":\"https://a.test/a.png\"")), "avatar");

		assertThat(get(PROFILE, member)).bodyJson().extractingPath("$.avatar").isEqualTo("fox");
	}

	// ---------- US6 플레이 가능 시간대 ----------

	@Test
	@DisplayName("US6-AC1 시간대 목록에서 고른 것이 저장되어 공통코드 순서로 보인다. 겹치면 하나, 목록 밖이면 V001(칸 playTimes)이다")
	void playTimes() {
		assertThat(get("/api/v1/codes/PLAY_TIME", member)).hasStatus(200).bodyJson().extractingPath("$[*].name")
			.asArray().containsExactly("평일 오전", "평일 저녁", "주말 종일", "새벽");

		assertThat(save(profile("\"playTimes\":[\"DAWN\",\"WEEKDAY_MORNING\",\"DAWN\"]"))).hasStatus(200);

		assertThat(get(PROFILE, member)).bodyJson().extractingPath("$.playTimes[*].code").asArray()
			.containsExactly("WEEKDAY_MORNING", "DAWN");
		assertFieldError(save(profile("\"playTimes\":[\"NOON\"]")), "playTimes");
		assertThat(get(PROFILE, member)).bodyJson().extractingPath("$.playTimes[*].code").asArray()
			.containsExactly("WEEKDAY_MORNING", "DAWN");
	}

	@Test
	@DisplayName("US6-AC2 시간대를 모두 풀고 저장하면 비어 있음으로 저장된다")
	void clearPlayTimes() {
		assertThat(save(profile("\"playTimes\":[\"DAWN\"]"))).hasStatus(200);

		assertThat(save(profile("\"playTimes\":[]"))).hasStatus(200);

		assertThat(get(PROFILE, member)).bodyJson().extractingPath("$.playTimes").asArray().isEmpty();
		assertThat(count("select count(*) from tb_user_profile_play_time")).isZero();
	}

	// ---------- Edge Cases ----------

	@Test
	@DisplayName("Edge 로그인하지 않으면 마이페이지 API는 모두 401 A001이다")
	void unauthenticated() {
		assertCode(mvc.get().uri(PROFILE).exchange(), 401, "A001");
		assertCode(mvc.get().uri(AVATARS).exchange(), 401, "A001");
		assertCode(mvc.put().uri(PROFILE).contentType(MediaType.APPLICATION_JSON).content(EMPTY_PROFILE).exchange(),
			401, "A001");
		assertCode(mvc.put().uri(NICKNAME).contentType(MediaType.APPLICATION_JSON)
			.content("{\"nickname\":\"새이름\"}").exchange(), 401, "A001");
	}

	@Test
	@DisplayName("Edge 지금과 같은 닉네임으로 저장하면 변경 제한 기간이 새로 시작하지 않는다")
	void sameNicknameDoesNotStartCooldown() {
		profileRow(MEMBER_ID, "moa_kim", "0421");

		MvcTestResult same = changeNickname(" moa_kim ");

		assertThat(same).hasStatus(200).bodyJson().extractingPath("$.nicknameTag").isEqualTo("0421");
		assertThat(nicknameChangedAt()).isNull();
	}

	@Test
	@DisplayName("Edge 대소문자만 바꾸면 태그가 그대로이고 변경 제한을 받는다")
	void caseOnlyChangeKeepsTag() {
		profileRow(MEMBER_ID, "moa_kim", "0421");

		assertThat(changeNickname("Moa_Kim")).bodyJson().extractingPath("$.nicknameTag").isEqualTo("0421");

		assertThat(text("select name from tb_user where id = ?", MEMBER_ID)).isEqualTo("Moa_Kim");
		assertCode(changeNickname("MOA_KIM"), 409, "B202");
	}

	@Test
	@DisplayName("Edge 프로필 행이 없는 회원의 조회는 태그 없이 기본값이고 행을 만들지 않는다")
	void readingDoesNotCreateRow() {
		MvcTestResult profile = get(PROFILE, member);

		assertThat(profile).hasStatus(200);
		assertThat(profile).bodyJson().extractingPath("$.nickname").isEqualTo("moa_kim");
		assertThat(profile).bodyJson().extractingPath("$.nicknameTag").isNull();
		assertThat(profile).bodyJson().extractingPath("$.discordVisibility").isEqualTo("PRIVATE");
		assertThat(count("select count(*) from tb_user_profile")).isZero();
	}

	@Test
	@DisplayName("Edge 프로필 행이 없는 회원이 처음 저장하면 지금 이름의 키와 태그로 행이 생긴다")
	void firstSaveCreatesRow() {
		assertThat(save(EMPTY_PROFILE)).hasStatus(200).bodyJson().extractingPath("$.nicknameTag").asString()
			.matches("\\d{4}");

		assertThat(text("select nickname_key from tb_user_profile where user_id = ?", MEMBER_ID)).isEqualTo("moa_kim");
		assertThat(nicknameChangedAt()).isNull();
	}

	@Test
	@DisplayName("Edge 프로필 행이 없는 회원이 동시에 두 번 처음 저장해도 둘 다 성공하고 행은 하나다")
	void concurrentFirstSave() throws Exception {
		for (long userId = 101L; userId <= 105L; userId++) {
			testUsers.create(userId, "first_" + userId, null, Role.USER);
			String token = bearer(userId, "USER");

			List<Integer> statuses = together(
				() -> put(PROFILE, token, profile("\"bio\":\"하나\"")).getResponse().getStatus(),
				() -> put(PROFILE, token, profile("\"bio\":\"둘\"")).getResponse().getStatus());

			assertThat(statuses).containsExactly(200, 200);
			assertThat(count("select count(*) from tb_user_profile where user_id = " + userId)).isEqualTo(1);
			assertThat(text("select bio from tb_user_profile where user_id = ?", userId)).isIn("하나", "둘");
		}
	}

	@Test
	@DisplayName("Edge 두 회원이 같은 닉네임으로 동시에 바꾸면 둘 다 바뀌고 서로 다른 태그를 갖는다")
	void concurrentSameNickname() throws Exception {
		testUsers.create(OTHER_ID, "moa_lee", null, Role.USER);
		profileRow(MEMBER_ID, "moa_kim", "0421");
		profileRow(OTHER_ID, "moa_lee", "0421");
		String other = bearer(OTHER_ID, "USER");

		List<Integer> statuses = together(
			() -> changeNickname("같은이름").getResponse().getStatus(),
			() -> put(NICKNAME, other, "{\"nickname\":\"같은이름\"}").getResponse().getStatus());

		assertThat(statuses).containsExactly(200, 200);
		assertThat(jdbcTemplate.queryForList("select nickname_tag from tb_user_profile where nickname_key = '같은이름'",
			String.class)).hasSize(2).doesNotHaveDuplicates();
	}

	@Test
	@DisplayName("Edge 두 창에서 닉네임을 동시에 바꾸면 먼저 저장된 쪽만 바뀌고, 나중 저장은 B202로 막힌다")
	void concurrentChangeByOneMember() throws Exception {
		profileRow(MEMBER_ID, "moa_kim", "0421");
		Map<String, MvcTestResult> results = new ConcurrentHashMap<>();

		List<Integer> statuses = together(
			() -> changeNickname("첫째창", results).getResponse().getStatus(),
			() -> changeNickname("둘째창", results).getResponse().getStatus());

		assertThat(statuses).containsExactlyInAnyOrder(200, 409);
		String saved = statuses.getFirst() == 200 ? "첫째창" : "둘째창";
		String blocked = statuses.getFirst() == 200 ? "둘째창" : "첫째창";
		assertCode(results.get(blocked), 409, "B202");
		assertThat(text("select name from tb_user where id = ?", MEMBER_ID)).isEqualTo(saved);
		assertThat(text("select nickname_key from tb_user_profile where user_id = ?", MEMBER_ID)).isEqualTo(saved);
		assertThat(nicknameChangedAt()).isNotNull();
	}

	@Test
	@DisplayName("Edge 공통코드에서 끈 시간대는 이미 고른 값은 이름과 함께 보이고, 새로 고를 수 없다")
	void disabledPlayTime() {
		assertThat(save(profile("\"playTimes\":[\"DAWN\"]"))).hasStatus(200);
		jdbcTemplate.update("update tb_code set enabled = false where group_code = 'PLAY_TIME' and code = 'DAWN'");

		assertThat(get(PROFILE, member)).bodyJson().extractingPath("$.playTimes[*].name").asArray()
			.containsExactly("새벽");
		assertFieldError(save(profile("\"playTimes\":[\"DAWN\"]")), "playTimes");
	}

	@Test
	@DisplayName("Edge 목록에서 빠진 아바타를 쓰던 회원에게는 default가 보인다")
	void removedAvatar() {
		profileRow(MEMBER_ID, "moa_kim", "0421");
		jdbcTemplate.update("update tb_user_profile set avatar_key = 'removed' where user_id = ?", MEMBER_ID);

		assertThat(get(PROFILE, member)).bodyJson().extractingPath("$.avatar").isEqualTo("default");
		assertThat(get(ME, member)).bodyJson().extractingPath("$.avatar").isEqualTo("default");
	}

	@Test
	@DisplayName("Edge 회원이 관리자 계정이 되면 마이페이지를 열 수 없고 적어 둔 프로필은 남는다")
	void memberBecomesManager() {
		assertThat(save(FULL_PROFILE)).hasStatus(200);
		jdbcTemplate.update("update tb_user set role = 'ADMIN' where id = ?", MEMBER_ID);

		assertCode(get(PROFILE, bearer(MEMBER_ID, "ADMIN")), 403, "A201");
		assertThat(text("select bio from tb_user_profile where user_id = ?", MEMBER_ID)).isEqualTo("같이 해요");
	}

	// ---------- 도우미 ----------

	private MvcTestResult get(String uri, String bearer) {
		return mvc.get().uri(uri).header(HttpHeaders.AUTHORIZATION, bearer).exchange();
	}

	private MvcTestResult put(String uri, String bearer, String json) {
		return mvc.put().uri(uri).header(HttpHeaders.AUTHORIZATION, bearer)
			.contentType(MediaType.APPLICATION_JSON).content(json).exchange();
	}

	private MvcTestResult save(String json) {
		return put(PROFILE, member, json);
	}

	private MvcTestResult changeNickname(String nickname) {
		return put(NICKNAME, member, "{\"nickname\":\"" + nickname + "\"}");
	}

	private MvcTestResult changeNickname(String nickname, Map<String, MvcTestResult> results) {
		MvcTestResult result = changeNickname(nickname);
		results.put(nickname, result);
		return result;
	}

	/** 이 칸만 채우고 나머지는 보내지 않는다 (null과 같다) */
	private static String profile(String fields) {
		return "{" + fields + "}";
	}

	/** 두 요청을 함께 시작해 상태 코드를 차례대로 준다 */
	private static List<Integer> together(Callable<Integer> first, Callable<Integer> second) throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Integer> firstStatus = executor.submit(() -> {
				start.await();
				return first.call();
			});
			Future<Integer> secondStatus = executor.submit(() -> {
				start.await();
				return second.call();
			});
			start.countDown();
			return List.of(firstStatus.get(), secondStatus.get());
		} finally {
			executor.shutdownNow();
		}
	}

	private void profileRow(long userId, String nicknameKey, String tag) {
		jdbcTemplate.update("insert into tb_user_profile (user_id, nickname_key, nickname_tag, created_at, updated_at) "
			+ "values (?, ?, ?, now(6), now(6))", userId, nicknameKey, tag);
	}

	private LocalDateTime nicknameChangedAt() {
		return jdbcTemplate.queryForObject("select nickname_changed_at from tb_user_profile where user_id = ?",
			LocalDateTime.class, MEMBER_ID);
	}

	private void nicknameChangedAt(LocalDateTime changedAt) {
		jdbcTemplate.update("update tb_user_profile set nickname_changed_at = ? where user_id = ?", changedAt,
			MEMBER_ID);
	}

	private String text(String sql, Object... args) {
		return jdbcTemplate.queryForObject(sql, String.class, args);
	}

	private int count(String sql) {
		return jdbcTemplate.queryForObject(sql, Integer.class);
	}

	private static String body(MvcTestResult result) throws Exception {
		return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
	}

	private static void assertCode(MvcTestResult result, int status, String code) {
		assertThat(result).hasStatus(status).bodyJson().extractingPath("$.code").isEqualTo(code);
	}

	private static void assertFieldError(MvcTestResult result, String field) {
		assertCode(result, 400, "V001");
		assertThat(result).bodyJson().extractingPath("$.errors[*].field").asArray().containsOnly(field);
	}
}
