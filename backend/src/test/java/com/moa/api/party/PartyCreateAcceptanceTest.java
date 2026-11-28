package com.moa.api.party;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
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
import com.moa.common.security.Role;
import com.moa.support.AcceptanceTest;
import com.moa.support.CodeSeed;
import com.moa.support.PartyFixtures;

/**
 * 파티 만들기. 스펙 specs/008-party US1의 수용 시나리오다 (contracts/party-api.md "모집 페이지와 파티").
 * 게임 목록(US1-AC5)은 마이그레이션 시드라, 테스트마다 CodeSeed로 되돌린다.
 */
class PartyCreateAcceptanceTest extends AcceptanceTest {

	private static final String PARTIES = "/api/v1/parties";
	private static final long MEMBER = 10L;
	private static final long OTHER = 11L;
	private static final long ADMIN = 50L;

	@Autowired
	private CodeSeed codeSeed;

	@Autowired
	private PartyFixtures partyFixtures;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void restoreCodes() {
		codeSeed.restore();
	}

	@Test
	@DisplayName("US1-AC1 회원이 공개 또는 승인으로 파티를 만들면 모집 중으로 생기고 모집 페이지에 바로 보이며, 만든 회원이 방장으로 멤버 1명이다")
	void createOpenAndApprovalParty() {
		partyFixtures.members(MEMBER, OTHER);

		MvcTestResult open = create(MEMBER, partyJson("OPEN", "\"같이 랭크\"", 5));
		MvcTestResult approval = create(OTHER, partyJson("APPROVAL", "\"승인 파티\"", 3));

		for (MvcTestResult result : List.of(open, approval)) {
			assertThat(result).hasStatus(201);
			assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("RECRUITING");
			assertThat(result).bodyJson().extractingPath("$.memberCount").isEqualTo(1);
			assertThat(result).bodyJson().extractingPath("$.members.length()").isEqualTo(1);
			assertThat(result).bodyJson().extractingPath("$.members[0].role").isEqualTo("HOST");
			assertThat(result).bodyJson().extractingPath("$.me.relation").isEqualTo("HOST");
		}
		assertThat(open).bodyJson().extractingPath("$.members[0].userId").isEqualTo((int)MEMBER);
		assertThat(open).bodyJson().extractingPath("$.host.displayName")
			.isEqualTo(PartyFixtures.displayName(MEMBER));
		assertThat(open).bodyJson().extractingPath("$.capacity").isEqualTo(5);
		assertThat(approval).bodyJson().extractingPath("$.joinPolicy").isEqualTo("APPROVAL");

		assertThat(mvc.get().uri(PARTIES).exchange()).hasStatus(200).bodyJson().extractingPath("$.content[*].id")
			.asArray().containsExactly(id(approval), id(open));
	}

	@Test
	@DisplayName("US1-AC2 비공개로 만들면 초대 코드(8자)와 초대 링크가 주어지고 모집 페이지에는 보이지 않는다")
	void createPrivateParty() {
		partyFixtures.member(MEMBER);

		MvcTestResult created = create(MEMBER, partyJson("PRIVATE", "\"비공개\"", 4));

		assertThat(created).hasStatus(201);
		String code = JsonPath.read(body(created), "$.invite.code");
		assertThat(code).hasSize(8);
		assertThat(created).bodyJson().extractingPath("$.invite.url").isEqualTo("/invite/" + code);
		assertThat(mvc.get().uri(PARTIES).exchange()).hasStatus(200).bodyJson().extractingPath("$.content")
			.asArray().isEmpty();
	}

	@Test
	@DisplayName("US1-AC3 제목이 비었거나, 인원이 1·11이거나, 목록에 없는 게임이면 V001로 막히고 그 칸을 알려 주며 파티가 생기지 않는다")
	void invalidFieldsAreRejected() {
		partyFixtures.member(MEMBER);

		assertInvalid(create(MEMBER, partyJson("OPEN", "\"\"", 5)), "title");
		assertInvalid(create(MEMBER, partyJson("OPEN", "\"인원 1\"", 1)), "capacity");
		assertInvalid(create(MEMBER, partyJson("OPEN", "\"인원 11\"", 11)), "capacity");
		assertInvalid(create(MEMBER, "{\"gameCode\":\"MINECRAFT\",\"title\":\"없는 게임\",\"description\":\"\","
			+ "\"capacity\":5,\"joinPolicy\":\"OPEN\"}"), "gameCode");

		assertThat(count("tb_party")).isZero();
		assertThat(count("tb_party_member")).isZero();
	}

	@Test
	@DisplayName("US1-AC4 로그인하지 않고 파티를 만들면 401 A001이다")
	void anonymousCannotCreate() {
		assertThat(mvc.post().uri(PARTIES).contentType(MediaType.APPLICATION_JSON)
			.content(partyJson("OPEN", "\"익명\"", 5)))
			.hasStatus(401).bodyJson().extractingPath("$.code").isEqualTo("A001");
		assertThat(count("tb_party")).isZero();
	}

	@Test
	@DisplayName("US1-AC5 고를 수 있는 게임은 리그 오브 레전드, 오버워치, 발로란트, 배틀그라운드 순서다")
	void gamesInOrder() {
		partyFixtures.member(MEMBER);

		MvcTestResult games = mvc.get().uri("/api/v1/codes/GAME")
			.header(HttpHeaders.AUTHORIZATION, bearer(MEMBER, Role.USER.name())).exchange();

		assertThat(games).hasStatus(200);
		assertThat(games).bodyJson().extractingPath("$[*].code").asArray()
			.containsExactly("LOL", "OVERWATCH", "VALORANT", "PUBG");
		assertThat(games).bodyJson().extractingPath("$[*].name").asArray()
			.containsExactly("리그 오브 레전드", "오버워치", "발로란트", "배틀그라운드");
	}

	@Test
	@DisplayName("US1-AC6 관리자 계정이 만들기, 참가, 신청, 초대 입장, 내 파티를 하면 각각 403 A103이고 멤버·신청 행이 생기지 않는다")
	void adminAccountCannotUseParties() {
		partyFixtures.admin(ADMIN);
		long open = partyFixtures.openParty(MEMBER, 5);
		long approval = partyFixtures.approvalParty(OTHER, 5);
		long privateParty = partyFixtures.privateParty(12L, 5);
		String admin = bearer(ADMIN, Role.ADMIN.name());

		List<MvcTestResult> results = List.of(
			mvc.post().uri(PARTIES).header(HttpHeaders.AUTHORIZATION, admin).contentType(MediaType.APPLICATION_JSON)
				.content(partyJson("OPEN", "\"관리자 파티\"", 5)).exchange(),
			mvc.post().uri(PARTIES + "/" + open + "/members").header(HttpHeaders.AUTHORIZATION, admin).exchange(),
			mvc.post().uri(PARTIES + "/" + approval + "/join-requests").header(HttpHeaders.AUTHORIZATION, admin)
				.exchange(),
			mvc.post().uri(PARTIES + "/invites/" + partyFixtures.inviteCode(privateParty) + "/members")
				.header(HttpHeaders.AUTHORIZATION, admin).exchange(),
			mvc.get().uri(PARTIES + "/me").header(HttpHeaders.AUTHORIZATION, admin).exchange());

		for (MvcTestResult result : results) {
			assertThat(result).hasStatus(403).bodyJson().extractingPath("$.code").isEqualTo("A103");
		}
		assertThat(partyFixtures.isInAnyParty(ADMIN)).isFalse();
		assertThat(partyFixtures.requestCount(ADMIN)).isZero();
		assertThat(count("tb_party")).isEqualTo(3);
	}

	private void assertInvalid(MvcTestResult result, String field) {
		assertThat(result).hasStatus(400);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("V001");
		assertThat(result).bodyJson().extractingPath("$.errors[*].field").asArray().contains(field);
	}

	private MvcTestResult create(long userId, String json) {
		return mvc.post().uri(PARTIES).header(HttpHeaders.AUTHORIZATION, bearer(userId, Role.USER.name()))
			.contentType(MediaType.APPLICATION_JSON).content(json).exchange();
	}

	/** 포지션 조건 없는 롤 파티. title은 JSON 값 그대로 */
	private static String partyJson(String joinPolicy, String title, int capacity) {
		return "{\"gameCode\":\"LOL\",\"title\":" + title + ",\"description\":\"소개\",\"capacity\":" + capacity
			+ ",\"joinPolicy\":\"" + joinPolicy + "\"}";
	}

	private int count(String table) {
		Integer count = jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
		return count == null ? 0 : count;
	}

	private int id(MvcTestResult result) {
		return JsonPath.read(body(result), "$.id");
	}

	private static String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
