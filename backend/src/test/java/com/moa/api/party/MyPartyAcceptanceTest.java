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
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;
import com.moa.support.AcceptanceTest;
import com.moa.support.CodeSeed;
import com.moa.support.PartyFixtures;

/**
 * 내 파티 (specs/008-party US8-AC1·AC2, contracts/party-api.md "내 파티").
 */
class MyPartyAcceptanceTest extends AcceptanceTest {

	private static final String PARTIES = "/api/v1/parties";
	private static final long HOST = 10L;
	private static final long OTHER_HOST = 11L;
	private static final long ME = 12L;

	@Autowired
	private CodeSeed codeSeed;

	@Autowired
	private PartyFixtures partyFixtures;

	private String me;

	@BeforeEach
	void setUp() {
		codeSeed.restore();
		partyFixtures.members(HOST, OTHER_HOST, ME);
		me = bearer(ME, "USER");
	}

	@Test
	@DisplayName("US8-AC1 파티에 속하지 않고 승인 파티 두 곳에 신청해 두면, 두 신청이 파티 제목·고른 포지션과 함께 신청순으로 나온다")
	void pendingRequests() {
		long positionParty = partyFixtures.party(HOST).title("첫 파티").joinPolicy("APPROVAL")
			.position("MID", 1).position("JUNGLE", 1).hostPosition("MID").create();
		long plainParty = partyFixtures.party(OTHER_HOST).title("둘째 파티").joinPolicy("APPROVAL").capacity(4)
			.create();
		assertThat(post(me, PARTIES + "/" + positionParty + "/join-requests", "{\"positionCode\":\"JUNGLE\"}"))
			.hasStatus(201);
		assertThat(post(me, PARTIES + "/" + plainParty + "/join-requests", "{}")).hasStatus(201);

		MvcTestResult result = get(me, PARTIES + "/me");

		assertThat(result).hasStatus(200);
		String body = body(result);
		assertThat(JsonPath.<Object>read(body, "$.party")).isNull();
		assertThat(JsonPath.<List<String>>read(body, "$.joinRequests[*].party.title"))
			.containsExactly("첫 파티", "둘째 파티");
		List<Number> partyIds = JsonPath.read(body, "$.joinRequests[*].party.id");
		assertThat(partyIds.stream().map(Number::longValue).toList()).containsExactly(positionParty, plainParty);
		assertThat(JsonPath.<String>read(body, "$.joinRequests[0].positionCode")).isEqualTo("JUNGLE");
		assertThat(JsonPath.<Object>read(body, "$.joinRequests[1].positionCode")).isNull();
		assertThat(JsonPath.<List<String>>read(body, "$.joinRequests[*].party.gameCode")).containsOnly("LOL");
		assertThat(JsonPath.<List<String>>read(body, "$.joinRequests[*].appliedAt"))
			.allSatisfy(at -> assertThat(at).isNotBlank());
	}

	@Test
	@DisplayName("US8-AC2 비공개 파티의 멤버가 내 파티를 열면 그 파티와 내 역할(MEMBER)이 나오고, 방장에게는 HOST로 나온다")
	void privateMembership() {
		long partyId = partyFixtures.privateParty(HOST, 4);
		assertThat(post(me, PARTIES + "/invites/" + partyFixtures.inviteCode(partyId) + "/members", "{}"))
			.hasStatus(200);

		MvcTestResult result = get(me, PARTIES + "/me");

		assertThat(result).hasStatus(200);
		String body = body(result);
		assertThat(JsonPath.<Number>read(body, "$.party.id").longValue()).isEqualTo(partyId);
		assertThat(JsonPath.<String>read(body, "$.party.joinPolicy")).isEqualTo("PRIVATE");
		assertThat(JsonPath.<String>read(body, "$.party.myRole")).isEqualTo("MEMBER");
		assertThat(JsonPath.<Integer>read(body, "$.party.memberCount")).isEqualTo(2);
		assertThat(JsonPath.<List<Object>>read(body, "$.joinRequests")).isEmpty();

		String hosted = body(get(bearer(HOST, "USER"), PARTIES + "/me"));
		assertThat(JsonPath.<String>read(hosted, "$.party.myRole")).isEqualTo("HOST");
	}

	// ---------- 도우미 ----------

	private MvcTestResult get(String bearer, String uri) {
		return mvc.get().uri(uri).header(HttpHeaders.AUTHORIZATION, bearer).exchange();
	}

	private MvcTestResult post(String bearer, String uri, String json) {
		return mvc.post().uri(uri).header(HttpHeaders.AUTHORIZATION, bearer).contentType(MediaType.APPLICATION_JSON)
			.content(json).exchange();
	}

	private static String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
