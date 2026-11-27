package com.moa.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.moa.api.service.user.NicknameTags;
import com.moa.common.security.Role;
import com.moa.support.AcceptanceTest;
import com.moa.support.CodeSeed;
import com.moa.support.DatabaseCleaner;
import com.moa.support.TestUsers;

/**
 * 태그 충돌 다시 하기 (specs/009-mypage FR-003, research R4·R14). 고른 태그를 다른 요청이 먼저 가져간 상황을 흉내 내려고
 * NicknameTags가 이미 쓰이는 태그를 주게 한다. 유일 제약 위반은 실제 DB가 낸다.
 * 같은 회원의 행 동시 생성(R002)은 ProfileServiceConflictTest와 MyPageAcceptanceTest가 본다.
 */
class MyPageRetryAcceptanceTest extends AcceptanceTest {

	private static final String PROFILE = "/api/v1/users/me/profile";
	private static final String NICKNAME = "/api/v1/users/me/nickname";
	private static final long MEMBER_ID = 10L;
	private static final long OTHER_ID = 11L;
	private static final String TAKEN = "0001";
	private static final String FREE = "0002";

	@MockitoSpyBean
	private NicknameTags nicknameTags;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private CodeSeed codeSeed;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DatabaseCleaner databaseCleaner;

	private String member;

	@BeforeEach
	void setUp() {
		databaseCleaner.clean();
		codeSeed.restore();
		testUsers.create(1L, "system", null, Role.USER);
		testUsers.create(MEMBER_ID, "moa_kim", null, Role.USER);
		testUsers.create(OTHER_ID, "other", null, Role.USER);
		member = bearer(MEMBER_ID, "USER");
	}

	@Test
	@DisplayName("Edge 닉네임을 바꿀 때 태그가 두 번 겹치면 다시 해서 세 번째 태그로 바뀐다")
	void changeNicknameRetries() {
		profileRow(MEMBER_ID, "moa_kim", "0421");
		profileRow(OTHER_ID, "같은이름", TAKEN);
		doReturn(TAKEN, TAKEN, FREE).when(nicknameTags).keepOrAssign(any(), any(), any());

		MvcTestResult changed = put(NICKNAME, "{\"nickname\":\"같은이름\"}");

		assertThat(changed).hasStatus(200).bodyJson().extractingPath("$.nicknameTag").isEqualTo(FREE);
		verify(nicknameTags, times(NicknameTags.MAX_ATTEMPTS)).keepOrAssign(any(), any(), any());
	}

	@Test
	@DisplayName("Edge 닉네임을 바꿀 때 태그가 세 번 모두 겹치면 B201이고 아무것도 바뀌지 않는다")
	void changeNicknameGivesUp() {
		profileRow(MEMBER_ID, "moa_kim", "0421");
		profileRow(OTHER_ID, "같은이름", TAKEN);
		doReturn(TAKEN).when(nicknameTags).keepOrAssign(any(), any(), any());

		MvcTestResult changed = put(NICKNAME, "{\"nickname\":\"같은이름\"}");

		assertThat(changed).hasStatus(409).bodyJson().extractingPath("$.code").isEqualTo("B201");
		verify(nicknameTags, times(NicknameTags.MAX_ATTEMPTS)).keepOrAssign(any(), any(), any());
		assertThat(text("select name from tb_user where id = ?")).isEqualTo("moa_kim");
		assertThat(text("select nickname_tag from tb_user_profile where user_id = ?")).isEqualTo("0421");
		assertThat(text("select nickname_changed_at from tb_user_profile where user_id = ?")).isNull();
	}

	@Test
	@DisplayName("Edge 프로필을 처음 저장할 때 태그가 두 번 겹치면 다시 해서 세 번째 태그로 행이 생긴다")
	void firstSaveRetries() {
		profileRow(OTHER_ID, "moa_kim", TAKEN);
		doReturn(TAKEN, TAKEN, FREE).when(nicknameTags).assign(any());

		MvcTestResult saved = put(PROFILE, "{\"bio\":\"처음\"}");

		assertThat(saved).hasStatus(200).bodyJson().extractingPath("$.nicknameTag").isEqualTo(FREE);
		verify(nicknameTags, times(NicknameTags.MAX_ATTEMPTS)).assign(any());
		assertThat(text("select bio from tb_user_profile where user_id = ?")).isEqualTo("처음");
	}

	@Test
	@DisplayName("Edge 프로필을 처음 저장할 때 태그가 세 번 모두 겹치면 B201이고 행이 생기지 않는다")
	void firstSaveGivesUp() {
		profileRow(OTHER_ID, "moa_kim", TAKEN);
		doReturn(TAKEN).when(nicknameTags).assign(any());

		MvcTestResult saved = put(PROFILE, "{\"bio\":\"처음\"}");

		assertThat(saved).hasStatus(409).bodyJson().extractingPath("$.code").isEqualTo("B201");
		verify(nicknameTags, times(NicknameTags.MAX_ATTEMPTS)).assign(any());
		assertThat(jdbcTemplate.queryForObject("select count(*) from tb_user_profile where user_id = ?", Integer.class,
			MEMBER_ID)).isZero();
	}

	private MvcTestResult put(String uri, String json) {
		return mvc.put().uri(uri).header(HttpHeaders.AUTHORIZATION, member)
			.contentType(MediaType.APPLICATION_JSON).content(json).exchange();
	}

	private void profileRow(long userId, String nicknameKey, String tag) {
		jdbcTemplate.update("insert into tb_user_profile (user_id, nickname_key, nickname_tag, created_at, updated_at) "
			+ "values (?, ?, ?, now(6), now(6))", userId, nicknameKey, tag);
	}

	private String text(String sql) {
		return jdbcTemplate.queryForObject(sql, String.class, MEMBER_ID);
	}
}
