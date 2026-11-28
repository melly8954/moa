package com.moa.api.party;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.moa.common.security.Role;
import com.moa.support.AcceptanceTest;
import com.moa.support.CodeSeed;
import com.moa.support.PartyFixtures;

/**
 * 공개 파티 참가. 스펙 specs/008-party US3의 수용 시나리오다 (contracts/party-api.md "참가와 나가기", "멤버가 되는 모든 길").
 * 파티는 PartyFixtures로 바로 넣고, 참가만 API로 부른다.
 */
class PartyJoinAcceptanceTest extends AcceptanceTest {

	private static final String PARTIES = "/api/v1/parties";
	private static final long HOST = 10L;
	private static final long JOINER = 20L;
	private static final long SECOND_JOINER = 21L;

	@Autowired
	private CodeSeed codeSeed;

	@Autowired
	private PartyFixtures partyFixtures;

	@BeforeEach
	void restoreCodes() {
		codeSeed.restore();
	}

	@Test
	@DisplayName("US3-AC1 자리가 남은 공개 파티에 참가하면 곧바로 멤버가 되고 현재 인원이 1 늘어난다")
	void joinOpenParty() {
		long partyId = partyFixtures.openParty(HOST, 3);
		partyFixtures.member(JOINER);

		MvcTestResult joined = join(JOINER, partyId);

		assertThat(joined).hasStatus(200);
		assertThat(joined).bodyJson().extractingPath("$.memberCount").isEqualTo(2);
		assertThat(joined).bodyJson().extractingPath("$.me.relation").isEqualTo("MEMBER");
		assertThat(joined).bodyJson().extractingPath("$.members[*].userId").asArray()
			.containsExactly((int)HOST, (int)JOINER);
		assertThat(joined).bodyJson().extractingPath("$.status").isEqualTo("RECRUITING");
		assertThat(partyFixtures.memberCount(partyId)).isEqualTo(2);
	}

	@Test
	@DisplayName("US3-AC2 마지막 자리가 차면 모집 마감(closedByHost false)이 되고 모집 페이지에서 빠진다")
	void lastSeatClosesParty() {
		long partyId = partyFixtures.openParty(HOST, 2);
		partyFixtures.member(JOINER);

		MvcTestResult joined = join(JOINER, partyId);

		assertThat(joined).hasStatus(200);
		assertThat(joined).bodyJson().extractingPath("$.status").isEqualTo("CLOSED");
		assertThat(joined).bodyJson().extractingPath("$.closedByHost").isEqualTo(false);
		assertThat(mvc.get().uri(PARTIES).exchange()).hasStatus(200).bodyJson().extractingPath("$.content")
			.asArray().isEmpty();
	}

	@Test
	@DisplayName("US3-AC3 정원이 찬 파티에 참가하면 409 B101, 방장이 마감한 파티면 409 B102다")
	void fullOrClosedPartyRejects() {
		long full = partyFixtures.openParty(HOST, 2);
		partyFixtures.addMember(full, 11L);
		partyFixtures.status(full, "CLOSED", false);
		long closed = partyFixtures.party(12L).capacity(5).closed(true).create();
		partyFixtures.member(JOINER);

		assertThat(join(JOINER, full)).hasStatus(409).bodyJson().extractingPath("$.code").isEqualTo("B101");
		assertThat(join(JOINER, closed)).hasStatus(409).bodyJson().extractingPath("$.code").isEqualTo("B102");
		assertThat(partyFixtures.isInAnyParty(JOINER)).isFalse();
	}

	@Test
	@DisplayName("US3-AC4 이미 멤버인 파티에 다시 참가하면 409 B103이다")
	void joinTwiceRejects() {
		long partyId = partyFixtures.openParty(HOST, 4);
		partyFixtures.member(JOINER);
		assertThat(join(JOINER, partyId)).hasStatus(200);

		assertThat(join(JOINER, partyId)).hasStatus(409).bodyJson().extractingPath("$.code").isEqualTo("B103");
		assertThat(partyFixtures.memberCount(partyId)).isEqualTo(2);
	}

	@Test
	@DisplayName("US3-AC5 한 자리 남은 공개 파티에 두 회원이 동시에 참가하면 한 명만 멤버가 되고 다른 한 명은 B101이다")
	void concurrentJoinAdmitsOnlyOne() throws Exception {
		long partyId = partyFixtures.openParty(HOST, 3);
		partyFixtures.addMember(partyId, 11L);
		partyFixtures.members(JOINER, SECOND_JOINER);

		List<MvcTestResult> results = together(() -> join(JOINER, partyId), () -> join(SECOND_JOINER, partyId));

		assertThat(results).extracting(result -> result.getResponse().getStatus())
			.containsExactlyInAnyOrder(200, 409);
		MvcTestResult rejected = results.stream().filter(result -> result.getResponse().getStatus() == 409)
			.findFirst().orElseThrow();
		assertThat(rejected).bodyJson().extractingPath("$.code").isEqualTo("B101");
		assertThat(partyFixtures.memberCount(partyId)).isEqualTo(3);
	}

	@Test
	@DisplayName("US3-AC6 다른 파티의 멤버(방장 포함)가 만들기, 참가, 신청, 초대 입장을 하면 각각 409 B103이다")
	void memberOfAnotherPartyIsRejected() {
		long own = partyFixtures.openParty(HOST, 4);
		long open = partyFixtures.openParty(11L, 4);
		long approval = partyFixtures.approvalParty(12L, 4);
		long privateParty = partyFixtures.privateParty(13L, 4);
		String host = bearer(HOST, Role.USER.name());

		List<MvcTestResult> results = List.of(
			mvc.post().uri(PARTIES).header(HttpHeaders.AUTHORIZATION, host).contentType(MediaType.APPLICATION_JSON)
				.content("{\"gameCode\":\"LOL\",\"title\":\"두 번째\",\"description\":\"\",\"capacity\":4,"
					+ "\"joinPolicy\":\"OPEN\"}")
				.exchange(),
			join(HOST, open),
			mvc.post().uri(PARTIES + "/" + approval + "/join-requests").header(HttpHeaders.AUTHORIZATION, host)
				.exchange(),
			mvc.post().uri(PARTIES + "/invites/" + partyFixtures.inviteCode(privateParty) + "/members")
				.header(HttpHeaders.AUTHORIZATION, host).exchange());

		for (MvcTestResult result : results) {
			assertThat(result).hasStatus(409).bodyJson().extractingPath("$.code").isEqualTo("B103");
		}
		assertThat(partyFixtures.memberCount(own)).isEqualTo(1);
		assertThat(partyFixtures.memberCount(open)).isEqualTo(1);
		assertThat(partyFixtures.memberCount(privateParty)).isEqualTo(1);
		assertThat(partyFixtures.requestCount(HOST)).isZero();
	}

	@Test
	@DisplayName("US3-AC7 승인 파티 두 곳에 신청해 둔 회원이 공개 파티에 참가하면 두 신청이 모두 취소(CANCELLED)된다")
	void joiningCancelsPendingRequests() {
		long first = partyFixtures.approvalParty(11L, 4);
		long second = partyFixtures.approvalParty(12L, 4);
		long firstRequest = partyFixtures.pendingRequest(first, JOINER);
		long secondRequest = partyFixtures.pendingRequest(second, JOINER);
		long open = partyFixtures.openParty(HOST, 4);

		assertThat(join(JOINER, open)).hasStatus(200);

		assertThat(partyFixtures.requestStatus(firstRequest)).isEqualTo("CANCELLED");
		assertThat(partyFixtures.requestStatus(secondRequest)).isEqualTo("CANCELLED");
		assertThat(mvc.get().uri(PARTIES + "/me").header(HttpHeaders.AUTHORIZATION, bearer(JOINER, Role.USER.name()))
			.exchange()).hasStatus(200).bodyJson().extractingPath("$.joinRequests").asArray().isEmpty();
	}

	private MvcTestResult join(long userId, long partyId) {
		return mvc.post().uri(PARTIES + "/" + partyId + "/members")
			.header(HttpHeaders.AUTHORIZATION, bearer(userId, Role.USER.name())).exchange();
	}

	/** 두 요청을 함께 시작해 결과를 차례대로 준다 (MyPageAcceptanceTest와 같은 방식) */
	private static List<MvcTestResult> together(Callable<MvcTestResult> first, Callable<MvcTestResult> second)
		throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<MvcTestResult> firstResult = executor.submit(() -> {
				start.await();
				return first.call();
			});
			Future<MvcTestResult> secondResult = executor.submit(() -> {
				start.await();
				return second.call();
			});
			start.countDown();
			return List.of(firstResult.get(), secondResult.get());
		} finally {
			executor.shutdownNow();
		}
	}
}
