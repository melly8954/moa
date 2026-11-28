package com.moa.api.party;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.moa.common.security.Role;
import com.moa.support.AcceptanceTest;
import com.moa.support.CodeSeed;
import com.moa.support.PartyFixtures;

/**
 * 모집 페이지와 파티 화면. 스펙 specs/008-party US2의 수용 시나리오다 (contracts/party-api.md PartySummary, PartyDetail).
 * 파티는 PartyFixtures로 바로 넣는다. 로그인 화면으로 보냈다 돌아오는 것(US2-AC6 뒷부분)은 e2e가 본다.
 */
class PartyBrowseAcceptanceTest extends AcceptanceTest {

	private static final String PARTIES = "/api/v1/parties";
	private static final long VIEWER = 30L;

	@Autowired
	private CodeSeed codeSeed;

	@Autowired
	private PartyFixtures partyFixtures;

	@BeforeEach
	void restoreCodes() {
		codeSeed.restore();
	}

	@Test
	@DisplayName("US2-AC1 모집 페이지에는 모집 중인 공개·승인 파티만 최신순으로 카드 항목과 함께 나오고, 마감·비공개 파티는 없다")
	void recruitPageShowsOpenAndApprovalOnly() {
		long open = partyFixtures.openParty(10L, 4);
		long approval = partyFixtures.approvalParty(11L, 4);
		partyFixtures.party(12L).closed(false).create();
		partyFixtures.privateParty(13L, 4);
		long positioned = partyFixtures.party(14L).mode("SOLO_RANK").tiers("GOLD", "DIAMOND").title("솔랭 구함")
			.position("MID", 1).hostPosition("MID").position("TOP", 1).position("JUNGLE", 1).create();
		partyFixtures.addMember(positioned, 15L, "TOP");
		partyFixtures.member(VIEWER);

		MvcTestResult page = viewer(mvc.get().uri(PARTIES));

		assertThat(page).hasStatus(200);
		assertThat(page).bodyJson().extractingPath("$.content[*].id").asArray()
			.containsExactly((int)positioned, (int)approval, (int)open);
		assertThat(page).bodyJson().extractingPath("$.content[0]").asMap().containsOnlyKeys("id", "gameCode",
			"modeCode", "title", "joinPolicy", "memberCount", "capacity", "openPositions", "tierMinCode",
			"tierMaxCode", "host", "createdAt");
		assertThat(page).bodyJson().extractingPath("$.content[0].gameCode").isEqualTo("LOL");
		assertThat(page).bodyJson().extractingPath("$.content[0].modeCode").isEqualTo("SOLO_RANK");
		assertThat(page).bodyJson().extractingPath("$.content[0].title").isEqualTo("솔랭 구함");
		assertThat(page).bodyJson().extractingPath("$.content[0].joinPolicy").isEqualTo("OPEN");
		assertThat(page).bodyJson().extractingPath("$.content[0].memberCount").isEqualTo(2);
		assertThat(page).bodyJson().extractingPath("$.content[0].capacity").isEqualTo(3);
		assertThat(page).bodyJson().extractingPath("$.content[0].openPositions[*].positionCode").asArray()
			.containsExactly("JUNGLE");
		assertThat(page).bodyJson().extractingPath("$.content[0].openPositions[0].remaining").isEqualTo(1);
		assertThat(page).bodyJson().extractingPath("$.content[0].tierMinCode").isEqualTo("GOLD");
		assertThat(page).bodyJson().extractingPath("$.content[0].tierMaxCode").isEqualTo("DIAMOND");
		assertThat(page).bodyJson().extractingPath("$.content[0].host").asMap()
			.containsOnlyKeys("userId", "displayName");
		assertThat(page).bodyJson().extractingPath("$.content[0].host.userId").isEqualTo(14);
		assertThat(page).bodyJson().extractingPath("$.content[0].host.displayName")
			.isEqualTo(PartyFixtures.displayName(14L));
		assertThat(page).bodyJson().extractingPath("$.content[1].joinPolicy").isEqualTo("APPROVAL");
	}

	@Test
	@DisplayName("US2-AC2 게임 하나로, 또는 게임과 모드로 거르면 조건에 맞는 파티만 나온다")
	void filterByGameAndMode() {
		long soloRank = partyFixtures.party(10L).mode("SOLO_RANK").create();
		long aram = partyFixtures.party(11L).mode("ARAM").create();
		partyFixtures.party(12L).game("OVERWATCH").mode("COMPETITIVE").create();

		assertThat(mvc.get().uri(PARTIES + "?gameCode=LOL").exchange()).hasStatus(200).bodyJson()
			.extractingPath("$.content[*].id").asArray().containsExactly((int)aram, (int)soloRank);
		assertThat(mvc.get().uri(PARTIES + "?gameCode=LOL&modeCode=SOLO_RANK").exchange()).hasStatus(200)
			.bodyJson().extractingPath("$.content[*].id").asArray().containsExactly((int)soloRank);
		// 모드만 주고 게임이 없으면 거르지 않고 막는다 (contracts/party-api.md)
		assertThat(mvc.get().uri(PARTIES + "?modeCode=SOLO_RANK").exchange()).hasStatus(400).bodyJson()
			.extractingPath("$.code").isEqualTo("V001");
	}

	@Test
	@DisplayName("US2-AC3 공개·승인 파티 화면에는 소개, 방장, 멤버(포지션·티어), 인원, 포지션별 빈자리, 모드, 티어 조건, 참가 방식, 상태가 나오고 비멤버에게 초대 코드는 없다")
	void partyDetailForNonMember() {
		long partyId = partyFixtures.party(10L).joinPolicy("APPROVAL").mode("FLEX_RANK").tiers("SILVER", "GOLD")
			.description("즐겜 파티").position("MID", 1).hostPosition("MID").position("TOP", 1).position("ADC", 1)
			.create();
		partyFixtures.addMember(partyId, 11L, "TOP");
		partyFixtures.member(VIEWER);

		MvcTestResult detail = viewer(mvc.get().uri(PARTIES + "/" + partyId));

		assertThat(detail).hasStatus(200);
		assertThat(detail).bodyJson().extractingPath("$").asMap().containsOnlyKeys("id", "gameCode", "modeCode",
			"title", "joinPolicy", "memberCount", "capacity", "openPositions", "tierMinCode", "tierMaxCode", "host",
			"createdAt", "description", "status", "closedByHost", "positionEnabled", "positions", "members", "me",
			"invite");
		assertThat(detail).bodyJson().extractingPath("$.description").isEqualTo("즐겜 파티");
		assertThat(detail).bodyJson().extractingPath("$.modeCode").isEqualTo("FLEX_RANK");
		assertThat(detail).bodyJson().extractingPath("$.tierMinCode").isEqualTo("SILVER");
		assertThat(detail).bodyJson().extractingPath("$.tierMaxCode").isEqualTo("GOLD");
		assertThat(detail).bodyJson().extractingPath("$.joinPolicy").isEqualTo("APPROVAL");
		assertThat(detail).bodyJson().extractingPath("$.status").isEqualTo("RECRUITING");
		assertThat(detail).bodyJson().extractingPath("$.closedByHost").isEqualTo(false);
		assertThat(detail).bodyJson().extractingPath("$.positionEnabled").isEqualTo(true);
		assertThat(detail).bodyJson().extractingPath("$.memberCount").isEqualTo(2);
		assertThat(detail).bodyJson().extractingPath("$.capacity").isEqualTo(3);
		assertThat(detail).bodyJson().extractingPath("$.host.userId").isEqualTo(10);
		assertThat(detail).bodyJson().extractingPath("$.positions[*].positionCode").asArray()
			.containsExactlyInAnyOrder("MID", "TOP", "ADC");
		assertThat(detail).bodyJson().extractingPath("$.positions[?(@.positionCode == 'TOP')].filled").asArray()
			.containsExactly(1);
		assertThat(detail).bodyJson().extractingPath("$.positions[?(@.positionCode == 'ADC')].filled").asArray()
			.containsExactly(0);
		assertThat(detail).bodyJson().extractingPath("$.openPositions[*].positionCode").asArray()
			.containsExactly("ADC");
		assertThat(detail).bodyJson().extractingPath("$.members[*].userId").asArray().containsExactly(10, 11);
		assertThat(detail).bodyJson().extractingPath("$.members[1]").asMap().containsOnlyKeys("userId",
			"displayName", "role", "positionCode", "tierCode", "joinedAt");
		assertThat(detail).bodyJson().extractingPath("$.members[1].displayName")
			.isEqualTo(PartyFixtures.displayName(11L));
		assertThat(detail).bodyJson().extractingPath("$.members[1].role").isEqualTo("MEMBER");
		assertThat(detail).bodyJson().extractingPath("$.members[1].positionCode").isEqualTo("TOP");
		assertThat(detail).bodyJson().extractingPath("$.me.relation").isEqualTo("NONE");
		assertThat(detail).bodyJson().extractingPath("$.invite").isNull();
	}

	@Test
	@DisplayName("US2-AC4 멤버가 아닌 회원이 비공개 파티를 열면 404 R001이다")
	void privatePartyIsHiddenFromNonMember() {
		long partyId = partyFixtures.privateParty(10L, 4);
		partyFixtures.member(VIEWER);

		assertThat(viewer(mvc.get().uri(PARTIES + "/" + partyId))).hasStatus(404).bodyJson()
			.extractingPath("$.code").isEqualTo("R001");
		assertThat(mvc.get().uri(PARTIES + "/" + partyId).exchange()).hasStatus(404).bodyJson()
			.extractingPath("$.code").isEqualTo("R001");
	}

	@Test
	@DisplayName("US2-AC5 해산된 파티를 열면 404 R001이다")
	void dissolvedPartyIsNotFound() {
		long partyId = partyFixtures.openParty(10L, 4);
		partyFixtures.dissolve(partyId);
		partyFixtures.member(VIEWER);

		assertThat(viewer(mvc.get().uri(PARTIES + "/" + partyId))).hasStatus(404).bodyJson()
			.extractingPath("$.code").isEqualTo("R001");
		assertThat(mvc.get().uri(PARTIES).exchange()).bodyJson().extractingPath("$.content").asArray().isEmpty();
	}

	@Test
	@DisplayName("US2-AC6 로그인하지 않아도 모집 페이지와 공개 파티 화면이 200이고, 관계는 NONE이며 초대 코드는 없다")
	void anonymousCanBrowse() {
		long partyId = partyFixtures.openParty(10L, 4);
		partyFixtures.addMember(partyId, 11L);

		assertThat(mvc.get().uri(PARTIES).exchange()).hasStatus(200).bodyJson().extractingPath("$.content[*].id")
			.asArray().containsExactly((int)partyId);
		MvcTestResult detail = mvc.get().uri(PARTIES + "/" + partyId).exchange();
		assertThat(detail).hasStatus(200);
		assertThat(detail).bodyJson().extractingPath("$.members[*].userId").asArray().containsExactly(10, 11);
		assertThat(detail).bodyJson().extractingPath("$.me.relation").isEqualTo("NONE");
		assertThat(detail).bodyJson().extractingPath("$.invite").isNull();
	}

	private MvcTestResult viewer(
		org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(VIEWER, Role.USER.name())).exchange();
	}
}
