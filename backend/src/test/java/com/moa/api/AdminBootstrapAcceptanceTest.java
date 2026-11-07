package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.moa.api.application.v1.user.UserApplication;
import com.moa.common.audit.SystemActor;
import com.moa.common.exception.ServiceException;
import com.moa.common.security.Role;
import com.moa.support.AcceptanceTest;
import com.moa.support.DatabaseCleaner;
import com.moa.support.TestUsers;

/**
 * 첫 관리자 만들기 (AdminBootstrapRunner가 부르는 UserApplication.bootstrapAdmin)
 */
class AdminBootstrapAcceptanceTest extends AcceptanceTest {

	@Autowired
	private UserApplication userApplication;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private DatabaseCleaner databaseCleaner;

	/** 로컬 계정 SQL의 admin이 남아 있을 수 있어 비우고 시작한다 */
	@BeforeEach
	void clean() {
		databaseCleaner.clean();
	}

	@Test
	@DisplayName("첫 관리자: ADMIN이 없으면 임시 비밀번호로 만들고, 로그인하면 비밀번호 변경 요구가 보인다")
	void create() {
		assertThat(userApplication.hasAdmin()).isFalse();

		assertThat(bootstrap("root", "temporary123!")).isTrue();

		assertThat(userApplication.hasAdmin()).isTrue();
		MvcTestResult login = mvc.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"loginId\":\"root\",\"password\":\"temporary123!\"}").exchange();
		assertThat(login).hasStatus(200);
		assertThat(mvc.get().uri("/api/v1/users/me").header("Authorization", "Bearer "
			+ com.jayway.jsonpath.JsonPath.read(body(login), "$.accessToken")))
			.bodyJson().extractingPath("$.passwordChangeRequired").isEqualTo(true);
	}

	@Test
	@DisplayName("첫 관리자: ADMIN이 이미 있으면 만들지 않는다 (환경 변수를 남겨 둬도 된다)")
	void skipWhenAdminExists() {
		testUsers.create(2L, "admin", "admin1234!", Role.ADMIN);

		assertThat(bootstrap("root", "temporary123!")).isFalse();
		assertThat(bootstrap("admin", "temporary123!")).isFalse();
	}

	@Test
	@DisplayName("첫 관리자: 같은 아이디의 일반 계정이 있으면 R002, 비밀번호가 8자보다 짧으면 V001 (시작 실패)")
	void misconfigured() {
		testUsers.create(3L, "root", "user1234!", Role.USER);

		assertThatThrownBy(() -> bootstrap("root", "temporary123!")).isInstanceOf(ServiceException.class)
			.hasMessageContaining("root");
		assertThatThrownBy(() -> bootstrap("other", "short")).isInstanceOf(ServiceException.class);
		assertThat(userApplication.hasAdmin()).isFalse();
	}

	private boolean bootstrap(String loginId, String password) {
		return SystemActor.call(() -> userApplication.bootstrapAdmin(loginId, password, null));
	}

	private String body(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString();
		} catch (java.io.UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
