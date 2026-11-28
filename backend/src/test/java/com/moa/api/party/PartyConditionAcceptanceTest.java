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
 * 모드·포지션·티어 조건 (specs/008-party US10-AC1~AC10, FR-021~FR-024, contracts/party-api.md "만들기·고치기 요청의 칸").
 * 코드는 V15 시드다 (CodeSeed가 테스트마다 되돌린다).
 */
class PartyConditionAcceptanceTest extends AcceptanceTest {

	private static final String PARTIES = "/api/v1/parties";
	private static final long HOST = 10L;
	private static final long FIRST = 11L;
	private static final long SECOND = 12L;
	private static final long THIRD = 13L;

	@Autowired
	private CodeSeed codeSeed;

	@Autowired
	private PartyFixtures partyFixtures;

	private String host;
	private String first;
	private String second;
	private String third;

	@BeforeEach
	void setUp() {
		codeSeed.restore();
		host = member(HOST);
		first = member(FIRST);
		second = member(SECOND);
		third = member(THIRD);
	}

	@Test
	@DisplayName("US10-AC1 롤에서 방장 MID, 모집 JUNGLE·TOP, 모드와 티어 범위를 고르면 인원이 3이고 포지션 자리가 모두 1이다")
	void lolPositions() {
		MvcTestResult result = post(host, PARTIES, "{\"gameCode\":\"LOL\",\"modeCode\":\"SOLO_RANK\","
			+ "\"tierMinCode\":\"SILVER\",\"tierMaxCode\":\"GOLD\",\"title\":\"솔랭\",\"joinPolicy\":\"OPEN\","
			+ "\"hostPositionCode\":\"MID\",\"positions\":[{\"positionCode\":\"JUNGLE\",\"slots\":1},"
			+ "{\"positionCode\":\"TOP\",\"slots\":1}]}");

		assertThat(result).hasStatus(201);
		String body = body(result);
		assertThat(JsonPath.<Integer>read(body, "$.capacity")).isEqualTo(3);
		assertThat(JsonPath.<Boolean>read(body, "$.positionEnabled")).isTrue();
		assertThat(JsonPath.<String>read(body, "$.modeCode")).isEqualTo("SOLO_RANK");
		assertThat(JsonPath.<String>read(body, "$.tierMinCode")).isEqualTo("SILVER");
		assertThat(JsonPath.<String>read(body, "$.tierMaxCode")).isEqualTo("GOLD");
		assertThat(JsonPath.<List<String>>read(body, "$.positions[*].positionCode"))
			.containsExactlyInAnyOrder("JUNGLE", "TOP", "MID");
		assertThat(JsonPath.<List<Integer>>read(body, "$.positions[*].slots")).containsOnly(1);
		assertThat(JsonPath.<List<Integer>>read(body, "$.positions[?(@.positionCode == 'MID')].filled"))
			.containsExactly(1);
		assertThat(JsonPath.<String>read(body, "$.members[0].positionCode")).isEqualTo("MID");
	}

	@Test
	@DisplayName("US10-AC2 오버워치에서 TANK 1, DAMAGE 2, SUPPORT 2면 인원이 자리의 합 5다")
	void overwatchSlots() {
		MvcTestResult result = post(host, PARTIES, "{\"gameCode\":\"OVERWATCH\",\"title\":\"경쟁전\","
			+ "\"joinPolicy\":\"OPEN\",\"hostPositionCode\":\"DAMAGE\",\"positions\":["
			+ "{\"positionCode\":\"TANK\",\"slots\":1},{\"positionCode\":\"DAMAGE\",\"slots\":2},"
			+ "{\"positionCode\":\"SUPPORT\",\"slots\":2}]}");

		assertThat(result).hasStatus(201);
		String body = body(result);
		assertThat(JsonPath.<Integer>read(body, "$.capacity")).isEqualTo(5);
		assertThat(JsonPath.<List<Integer>>read(body, "$.positions[?(@.positionCode == 'DAMAGE')].slots"))
			.containsExactly(2);
		assertThat(JsonPath.<List<Integer>>read(body, "$.positions[?(@.positionCode == 'DAMAGE')].filled"))
			.containsExactly(1);
	}

	@Test
	@DisplayName("US10-AC3 조건을 하나도 걸지 않으면 positionEnabled가 false이고 모드·티어·포지션이 비어 있다")
	void noConditions() {
		MvcTestResult result = post(host, PARTIES,
			"{\"gameCode\":\"LOL\",\"title\":\"아무나\",\"capacity\":3,\"joinPolicy\":\"OPEN\"}");

		assertThat(result).hasStatus(201);
		String body = body(result);
		assertThat(JsonPath.<Boolean>read(body, "$.positionEnabled")).isFalse();
		assertThat(JsonPath.<List<Object>>read(body, "$.positions")).isEmpty();
		assertThat(JsonPath.<Object>read(body, "$.modeCode")).isNull();
		assertThat(JsonPath.<Object>read(body, "$.tierMinCode")).isNull();
		assertThat(JsonPath.<String>read(body, "$.members[0].positionCode")).isNull();
	}

	@Test
	@DisplayName("US10-AC4 배그에 포지션을 주면 400 V001(칸 positions)이고, 모드와 티어만 주면 만들어진다")
	void pubgHasNoPositions() {
		MvcTestResult withPositions = post(host, PARTIES, "{\"gameCode\":\"PUBG\",\"title\":\"배그\","
			+ "\"joinPolicy\":\"OPEN\",\"hostPositionCode\":\"TOP\",\"positions\":[{\"positionCode\":\"TOP\","
			+ "\"slots\":1}]}");

		assertCode(withPositions, 400, "V001");
		assertThat(JsonPath.<List<String>>read(body(withPositions), "$.errors[*].field")).contains("positions");
		assertThat(partyFixtures.isInAnyParty(HOST)).isFalse();

		MvcTestResult modeAndTier = post(host, PARTIES, "{\"gameCode\":\"PUBG\",\"modeCode\":\"RANKED\","
			+ "\"tierMinCode\":\"BRONZE\",\"tierMaxCode\":\"GOLD\",\"title\":\"배그\",\"capacity\":4,"
			+ "\"joinPolicy\":\"OPEN\"}");

		assertThat(modeAndTier).hasStatus(201);
		assertThat(JsonPath.<Boolean>read(body(modeAndTier), "$.positionEnabled")).isFalse();
	}

	@Test
	@DisplayName("US10-AC5 빈 포지션을 골라 참가·신청·초대 입장하면 그 포지션의 filled가 1 늘어난다. 신청은 승인된 뒤에 는다")
	void choosePosition() {
		long partyId = lolParty(host, "OPEN", "MID", "JUNGLE", "TOP", "ADC");

		assertThat(post(first, PARTIES + "/" + partyId + "/members", "{\"positionCode\":\"JUNGLE\"}"))
			.hasStatus(200);
		assertThat(filled(partyId, "JUNGLE")).isEqualTo(1);

		assertThat(put(host, PARTIES + "/" + partyId + "/join-policy", "{\"joinPolicy\":\"APPROVAL\"}"))
			.hasStatus(200);
		MvcTestResult applied = post(second, PARTIES + "/" + partyId + "/join-requests", "{\"positionCode\":\"TOP\"}");
		assertThat(applied).hasStatus(201);
		assertThat(filled(partyId, "TOP")).isZero();
		long requestId = ((Number)JsonPath.read(body(applied), "$.joinRequestId")).longValue();
		assertThat(post(host, PARTIES + "/" + partyId + "/join-requests/" + requestId + "/approve", ""))
			.hasStatus(200);
		assertThat(filled(partyId, "TOP")).isEqualTo(1);

		assertThat(post(third, PARTIES + "/invites/" + partyFixtures.inviteCode(partyId) + "/members",
			"{\"positionCode\":\"ADC\"}")).hasStatus(200);
		assertThat(filled(partyId, "ADC")).isEqualTo(1);
		String detail = body(get(host, PARTIES + "/" + partyId));
		assertThat(JsonPath.<List<String>>read(detail, "$.members[*].positionCode"))
			.containsExactly("MID", "JUNGLE", "TOP", "ADC");
	}

	@Test
	@DisplayName("US10-AC6 자리가 찬 포지션으로 들어오면 409 B105이고 details.openPositions에 남은 포지션이 있다")
	void fullPosition() {
		long partyId = lolParty(host, "OPEN", "MID", "JUNGLE", "TOP");
		assertThat(post(first, PARTIES + "/" + partyId + "/members", "{\"positionCode\":\"JUNGLE\"}"))
			.hasStatus(200);

		MvcTestResult result = post(second, PARTIES + "/" + partyId + "/members", "{\"positionCode\":\"JUNGLE\"}");

		assertCode(result, 409, "B105");
		assertThat(JsonPath.<List<String>>read(body(result), "$.details.openPositions")).containsExactly("TOP");
		assertThat(partyFixtures.isInAnyParty(SECOND)).isFalse();
	}

	@Test
	@DisplayName("US10-AC7 포지션 조건이 있는 파티에 포지션을 고르지 않고 참가·신청·초대 입장하면 400 B106이다")
	void positionRequired() {
		long open = lolParty(host, "OPEN", "MID", "JUNGLE", "TOP");
		assertCode(post(first, PARTIES + "/" + open + "/members", "{}"), 400, "B106");
		assertCode(post(first, PARTIES + "/invites/" + partyFixtures.inviteCode(open) + "/members", "{}"), 400,
			"B106");

		String otherHost = member(20L);
		long approval = lolParty(otherHost, "APPROVAL", "MID", "JUNGLE", "TOP");
		assertCode(post(first, PARTIES + "/" + approval + "/join-requests", "{}"), 400, "B106");
		assertThat(partyFixtures.requestCount(FIRST)).isZero();
	}

	@Test
	@DisplayName("US10-AC8 티어 범위 밖의 티어를 적어도 200이고 멤버 목록에 그 티어가 보인다. 그 게임에 없는 티어는 V001이다")
	void tierOutsideRange() {
		MvcTestResult created = post(host, PARTIES, "{\"gameCode\":\"LOL\",\"tierMinCode\":\"SILVER\","
			+ "\"tierMaxCode\":\"GOLD\",\"title\":\"실골\",\"capacity\":4,\"joinPolicy\":\"OPEN\"}");
		long partyId = ((Number)JsonPath.read(body(created), "$.id")).longValue();

		MvcTestResult result = post(first, PARTIES + "/" + partyId + "/members", "{\"tierCode\":\"CHALLENGER\"}");

		assertThat(result).hasStatus(200);
		assertThat(JsonPath.<List<String>>read(body(result), "$.members[?(@.userId == " + FIRST + ")].tierCode"))
			.containsExactly("CHALLENGER");
		assertCode(post(second, PARTIES + "/" + partyId + "/members", "{\"tierCode\":\"TOP500\"}"), 400, "V001");
	}

	@Test
	@DisplayName("US10-AC9 포지션을 차지한 멤버가 나가거나 내보내지면 그 포지션이 다시 빈자리가 된다")
	void positionFreedOnLeave() {
		long partyId = lolParty(host, "OPEN", "MID", "JUNGLE", "TOP");
		assertThat(post(first, PARTIES + "/" + partyId + "/members", "{\"positionCode\":\"JUNGLE\"}"))
			.hasStatus(200);
		assertThat(post(second, PARTIES + "/" + partyId + "/members", "{\"positionCode\":\"TOP\"}")).hasStatus(200);

		assertThat(delete(first, PARTIES + "/" + partyId + "/members/me")).hasStatus(204);
		assertThat(delete(host, PARTIES + "/" + partyId + "/members/" + SECOND)).hasStatus(204);

		assertThat(filled(partyId, "JUNGLE")).isZero();
		assertThat(filled(partyId, "TOP")).isZero();
		String detail = body(get(host, PARTIES + "/" + partyId));
		assertThat(JsonPath.<List<String>>read(detail, "$.openPositions[*].positionCode"))
			.containsExactlyInAnyOrder("JUNGLE", "TOP");
		assertThat(post(third, PARTIES + "/" + partyId + "/members", "{\"positionCode\":\"JUNGLE\"}"))
			.hasStatus(200);
	}

	@Test
	@DisplayName("US10-AC10 멤버가 차지한 포지션을 빼거나 그 자리를 멤버 수보다 줄이면 409 B110, 멤버가 있는 포지션 파티의 게임을 바꾸면 409 B111이다")
	void occupiedPositionLocked() {
		long lol = lolParty(host, "OPEN", "MID", "JUNGLE", "TOP");
		assertThat(post(first, PARTIES + "/" + lol + "/members", "{\"positionCode\":\"JUNGLE\"}")).hasStatus(200);

		assertCode(put(host, PARTIES + "/" + lol, "{\"gameCode\":\"LOL\",\"title\":\"롤\","
			+ "\"hostPositionCode\":\"MID\",\"positions\":[{\"positionCode\":\"TOP\",\"slots\":1}]}"), 409, "B110");
		assertCode(put(host, PARTIES + "/" + lol, "{\"gameCode\":\"OVERWATCH\",\"title\":\"롤\","
			+ "\"hostPositionCode\":\"TANK\",\"positions\":[{\"positionCode\":\"DAMAGE\",\"slots\":2}]}"), 409,
			"B111");

		String owHost = member(20L);
		MvcTestResult created = post(owHost, PARTIES, "{\"gameCode\":\"OVERWATCH\",\"title\":\"옵치\","
			+ "\"joinPolicy\":\"OPEN\",\"hostPositionCode\":\"TANK\",\"positions\":["
			+ "{\"positionCode\":\"TANK\",\"slots\":1},{\"positionCode\":\"DAMAGE\",\"slots\":2}]}");
		long ow = ((Number)JsonPath.read(body(created), "$.id")).longValue();
		assertThat(post(second, PARTIES + "/" + ow + "/members", "{\"positionCode\":\"DAMAGE\"}")).hasStatus(200);
		assertThat(post(third, PARTIES + "/" + ow + "/members", "{\"positionCode\":\"DAMAGE\"}")).hasStatus(200);

		assertCode(put(owHost, PARTIES + "/" + ow, "{\"gameCode\":\"OVERWATCH\",\"title\":\"옵치\","
			+ "\"hostPositionCode\":\"TANK\",\"positions\":[{\"positionCode\":\"TANK\",\"slots\":1},"
			+ "{\"positionCode\":\"DAMAGE\",\"slots\":1},{\"positionCode\":\"SUPPORT\",\"slots\":1}]}"), 409, "B110");
		assertThat(filled(ow, "DAMAGE")).isEqualTo(2);
	}

	@Test
	@DisplayName("FR-021 그 게임에 없는 모드, 최소 티어가 최대 티어보다 높음, 롤 포지션 2자리는 400 V001이다")
	void invalidConditions() {
		MvcTestResult wrongMode = post(host, PARTIES, "{\"gameCode\":\"LOL\",\"modeCode\":\"COMPETITIVE\","
			+ "\"title\":\"롤\",\"capacity\":4,\"joinPolicy\":\"OPEN\"}");
		assertCode(wrongMode, 400, "V001");
		assertThat(JsonPath.<List<String>>read(body(wrongMode), "$.errors[*].field")).contains("modeCode");

		MvcTestResult reversedTier = post(host, PARTIES, "{\"gameCode\":\"LOL\",\"tierMinCode\":\"GOLD\","
			+ "\"tierMaxCode\":\"SILVER\",\"title\":\"롤\",\"capacity\":4,\"joinPolicy\":\"OPEN\"}");
		assertCode(reversedTier, 400, "V001");
		assertThat(JsonPath.<List<String>>read(body(reversedTier), "$.errors[*].field")).contains("tierMaxCode");

		MvcTestResult twoSlots = post(host, PARTIES, "{\"gameCode\":\"LOL\",\"title\":\"롤\",\"joinPolicy\":\"OPEN\","
			+ "\"hostPositionCode\":\"MID\",\"positions\":[{\"positionCode\":\"JUNGLE\",\"slots\":2}]}");
		assertCode(twoSlots, 400, "V001");
		assertThat(JsonPath.<List<String>>read(body(twoSlots), "$.errors[*].field")).contains("positions");

		assertThat(partyFixtures.isInAnyParty(HOST)).isFalse();
	}

	// ---------- 도우미 ----------

	private String member(long userId) {
		partyFixtures.member(userId);
		return bearer(userId, "USER");
	}

	/** API로 만든 롤 포지션 파티. 모집할 포지션은 각 1자리다 */
	private long lolParty(String bearer, String joinPolicy, String hostPosition, String... openPositions) {
		StringBuilder positions = new StringBuilder();
		for (String position : openPositions) {
			positions.append(positions.isEmpty() ? "" : ",")
				.append("{\"positionCode\":\"").append(position).append("\",\"slots\":1}");
		}
		MvcTestResult result = post(bearer, PARTIES, "{\"gameCode\":\"LOL\",\"title\":\"롤\",\"joinPolicy\":\""
			+ joinPolicy + "\",\"hostPositionCode\":\"" + hostPosition + "\",\"positions\":[" + positions + "]}");
		assertThat(result).hasStatus(201);
		return ((Number)JsonPath.read(body(result), "$.id")).longValue();
	}

	private int filled(long partyId, String positionCode) {
		List<Integer> filled = JsonPath.read(body(get(host, PARTIES + "/" + partyId)),
			"$.positions[?(@.positionCode == '" + positionCode + "')].filled");
		assertThat(filled).hasSize(1);
		return filled.getFirst();
	}

	private MvcTestResult get(String bearer, String uri) {
		return mvc.get().uri(uri).header(HttpHeaders.AUTHORIZATION, bearer).exchange();
	}

	private MvcTestResult post(String bearer, String uri, String json) {
		return mvc.post().uri(uri).header(HttpHeaders.AUTHORIZATION, bearer).contentType(MediaType.APPLICATION_JSON)
			.content(json).exchange();
	}

	private MvcTestResult put(String bearer, String uri, String json) {
		return mvc.put().uri(uri).header(HttpHeaders.AUTHORIZATION, bearer).contentType(MediaType.APPLICATION_JSON)
			.content(json).exchange();
	}

	private MvcTestResult delete(String bearer, String uri) {
		return mvc.delete().uri(uri).header(HttpHeaders.AUTHORIZATION, bearer).exchange();
	}

	private static void assertCode(MvcTestResult result, int status, String code) {
		assertThat(result).hasStatus(status).bodyJson().extractingPath("$.code").isEqualTo(code);
	}

	private static String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
