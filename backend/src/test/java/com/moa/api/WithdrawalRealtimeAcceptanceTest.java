package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

import com.moa.api.realtime.BlockedAccountStompInterceptor;
import com.moa.common.security.Role;
import com.moa.common.websocket.StompAuthInterceptor;
import com.moa.support.RealtimeAcceptanceTest;
import com.moa.support.TestUsers;

/**
 * 탈퇴한 회원의 실시간 연결 (specs/018-member-withdrawal FR-006, research R3).
 */
class WithdrawalRealtimeAcceptanceTest extends RealtimeAcceptanceTest {

	private static final long MEMBER = 10L;
	private static final long OTHER = 11L;

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

	private static int indexOf(List<ChannelInterceptor> interceptors, Class<?> type) {
		for (int i = 0; i < interceptors.size(); i++) {
			if (type.isInstance(interceptors.get(i))) {
				return i;
			}
		}
		return -1;
	}
}
