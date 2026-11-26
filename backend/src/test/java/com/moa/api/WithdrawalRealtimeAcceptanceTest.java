package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.support.AbstractMessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.moa.api.realtime.BlockedAccountStompInterceptor;
import com.moa.common.security.Role;
import com.moa.common.security.access.AccountAccessChecker;
import com.moa.common.websocket.StompAuthInterceptor;
import com.moa.support.RealtimeAcceptanceTest;
import com.moa.support.TestUsers;

/**
 * 탈퇴한 회원의 실시간 연결 (specs/018-member-withdrawal FR-006, research R3).
 */
class WithdrawalRealtimeAcceptanceTest extends RealtimeAcceptanceTest {

	private static final long MEMBER = 10L;
	private static final long OTHER = 11L;

	/** 연결 직후 탈퇴를 만들려고 첫 확인만 바꾼다. 다른 시험에서는 그대로 돈다 */
	@MockitoSpyBean
	private AccountAccessChecker accountAccessChecker;

	@Autowired
	@Qualifier("clientInboundChannel")
	private AbstractMessageChannel clientInboundChannel;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("R3 막힌 계정 확인은 키트 인증 인터셉터 다음, 들어오는 채널의 맨 끝에서 돈다")
	void blockedAccountCheckRunsAfterKitAuth() {
		List<ChannelInterceptor> interceptors = clientInboundChannel.getInterceptors();
		int kit = indexOf(interceptors, StompAuthInterceptor.class);
		int moa = indexOf(interceptors, BlockedAccountStompInterceptor.class);

		assertThat(kit).isNotNegative();
		assertThat(moa).isGreaterThan(kit).isEqualTo(interceptors.size() - 1);
	}

	@Test
	@DisplayName("FR-006 탈퇴한 회원은 유효한 토큰으로도 연결할 수 없고, 활동 중인 회원은 연결된다")
	void withdrawnMemberCannotConnect() throws Exception {
		testUsers.create(MEMBER, "member", null, Role.USER);
		StompSession session = connected(MEMBER, Role.USER.name());
		assertThat(session.isConnected()).isTrue();
		session.disconnect();

		jdbcTemplate.update("update tb_user set withdrawn_at = now(6) where id = ?", MEMBER);

		assertThatThrownBy(() -> connect(bearer(MEMBER, Role.USER.name())).get(5, TimeUnit.SECONDS))
			.isInstanceOf(ExecutionException.class);
	}

	@Test
	@DisplayName("FR-006 탈퇴하면 이 서버에 열린 그 회원의 연결이 닫히고, 다른 회원의 연결은 그대로다")
	void withdrawalClosesOpenConnections() throws Exception {
		testUsers.create(MEMBER, "member", null, Role.USER);
		testUsers.create(OTHER, "other", null, Role.USER);
		StompSession first = connected(MEMBER, Role.USER.name());
		StompSession second = connected(MEMBER, Role.USER.name());
		StompSession other = connected(OTHER, Role.USER.name());

		assertThat(mvc.post().uri("/api/v1/users/me/withdrawal")
			.header(HttpHeaders.AUTHORIZATION, bearer(MEMBER, Role.USER.name()))
			.contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"member\"}").exchange()).hasStatus(200);

		long deadline = System.currentTimeMillis() + 5000;
		while ((first.isConnected() || second.isConnected()) && System.currentTimeMillis() < deadline) {
			Thread.sleep(50);
		}
		assertThat(first.isConnected()).isFalse();
		assertThat(second.isConnected()).isFalse();
		assertThat(other.isConnected()).isTrue();
		other.disconnect();
	}

	@Test
	@DisplayName("FR-006 CONNECT 확인을 지난 직후 탈퇴가 커밋돼 닫기를 비껴간 연결도, 연결이 맺어질 때 다시 확인해 닫는다")
	void withdrawnRightAfterConnectCheckIsClosed() throws Exception {
		testUsers.create(MEMBER, "member", null, Role.USER);
		jdbcTemplate.update("update tb_user set withdrawn_at = ? where id = ?", LocalDateTime.now(), MEMBER);
		// CONNECT 인터셉터가 물을 때는 아직 탈퇴 전이었던 것으로 둔다 (그 뒤 탈퇴가 커밋된 상황)
		doReturn(false).doCallRealMethod().when(accountAccessChecker).isBlocked(MEMBER);

		// 서버가 CONNECTED를 보낸 직후 닫으므로, 클라이언트는 연결이 맺어지기 전에 끊기거나 맺어진 직후 끊긴다. 둘 다 연결이 남지 않는다
		StompSession session;
		try {
			session = connect(bearer(MEMBER, Role.USER.name())).get(5, TimeUnit.SECONDS);
		} catch (ExecutionException closedBeforeConnected) {
			return;
		}
		long deadline = System.currentTimeMillis() + 5000;
		while (session.isConnected() && System.currentTimeMillis() < deadline) {
			Thread.sleep(50);
		}
		assertThat(session.isConnected()).isFalse();
	}

	private static int indexOf(List<ChannelInterceptor> interceptors, Class<?> type) {
		for (int i = 0; i < interceptors.size(); i++) {
			if (type.isInstance(interceptors.get(i))) {
				return i;
			}
		}
		return -1;
	}
}
