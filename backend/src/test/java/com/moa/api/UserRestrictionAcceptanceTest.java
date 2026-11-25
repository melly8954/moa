package com.moa.api;

import static com.moa.api.entity.user.QUser.user;
import static com.moa.api.entity.user.QUserSocialAccount.userSocialAccount;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.moa.api.dto.user.MemberStatusDto;
import com.moa.api.entity.user.User;
import com.moa.api.repository.jpa.user.UserRepository;
import com.moa.api.service.user.MemberStatusService;
import com.moa.common.security.Role;
import com.moa.support.AcceptanceTest;
import com.moa.support.DatabaseCleaner;
import com.moa.support.TestUsers;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;

/**
 * User의 기본 조회 조건(@SQLRestriction)이 조회 종류마다 붙는지 (specs/018-member-withdrawal research R8, tasks T002·T013).
 * 유예 중인 회원을 이 조건 하나로 숨기는 설계가 기대는 동작이다. 지운 회원(DELETED)과 유예 중인 회원(WITHDRAWN)은 어떤 JPA 조회에도
 * 나오지 않고, native 쿼리만 예외다 (로그인·복구·파기가 쓴다).
 */
class UserRestrictionAcceptanceTest extends AcceptanceTest {

	private static final long VISIBLE = 10L;
	private static final long DELETED = 11L;
	private static final long WITHDRAWN = 12L;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private MemberStatusService memberStatusService;

	@Autowired
	private JPAQueryFactory queryFactory;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private TestUsers testUsers;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DatabaseCleaner databaseCleaner;

	@BeforeEach
	void setUp() {
		databaseCleaner.clean();
		testUsers.create(1L, "system", null, Role.USER);
		for (long id : List.of(VISIBLE, DELETED, WITHDRAWN)) {
			testUsers.create(id, "member_" + id, null, Role.USER);
			jdbcTemplate.update("insert into tb_user_social_account (user_id, provider, subject, email, display_name, "
				+ "created_at) values (?, 'GOOGLE', ?, ?, null, now(6))", id, "subject-" + id, id + "@example.com");
		}
		jdbcTemplate.update("update tb_user set deleted_at = now(6), deleted_by = 1 where id = ?", DELETED);
		jdbcTemplate.update("update tb_user set withdrawn_at = now(6) where id = ?", WITHDRAWN);
	}

	@Test
	@DisplayName("R8 findById: 조건에 걸린 회원은 나오지 않는다")
	void findById() {
		assertThat(userRepository.findById(VISIBLE)).isPresent();
		assertThat(userRepository.findById(DELETED)).isEmpty();
		assertThat(userRepository.findById(WITHDRAWN)).isEmpty();
	}

	@Test
	@DisplayName("R8 메서드 이름 쿼리: 조건에 걸린 회원은 나오지 않는다")
	void derivedQuery() {
		jdbcTemplate.update("update tb_user set role = 'ADMIN' where id in (?, ?)", DELETED, WITHDRAWN);
		assertThat(userRepository.existsByRole(Role.ADMIN)).isFalse();

		jdbcTemplate.update("update tb_user set role = 'ADMIN' where id = ?", VISIBLE);
		assertThat(userRepository.existsByRole(Role.ADMIN)).isTrue();
	}

	@Test
	@DisplayName("R8 QueryDSL selectFrom: 조건에 걸린 회원은 나오지 않는다")
	void selectFrom() {
		assertThat(queryFactory.select(user.id).from(user).where(user.id.in(VISIBLE, DELETED, WITHDRAWN)).fetch())
			.containsExactly(VISIBLE);
	}

	@Test
	@DisplayName("R8 왼쪽 조인: 행은 남고 조건에 걸린 회원의 값은 비어 있다")
	void leftJoin() {
		List<Tuple> rows = queryFactory.select(userSocialAccount.userId, user.id, user.name)
			.from(userSocialAccount)
			.leftJoin(user).on(user.id.eq(userSocialAccount.userId))
			.orderBy(userSocialAccount.userId.asc())
			.fetch();

		assertThat(rows).extracting(row -> row.get(userSocialAccount.userId)).containsExactly(VISIBLE, DELETED,
			WITHDRAWN);
		assertThat(rows).extracting(row -> row.get(user.id)).containsExactly(VISIBLE, null, null);
		assertThat(rows).extracting(row -> row.get(user.name)).containsExactly("member_" + VISIBLE, null, null);
	}

	@Test
	@DisplayName("R8 안쪽 조인: 조건에 걸린 회원의 행이 빠진다")
	void innerJoin() {
		assertThat(queryFactory.select(userSocialAccount.userId)
			.from(userSocialAccount)
			.join(user).on(user.id.eq(userSocialAccount.userId))
			.fetch()).containsExactly(VISIBLE);
	}

	@Test
	@DisplayName("R8 서브쿼리: 조건에 걸린 회원은 나오지 않는다")
	void subquery() {
		assertThat(queryFactory.select(userSocialAccount.userId)
			.from(userSocialAccount)
			.where(userSocialAccount.userId.in(JPAExpressions.select(user.id).from(user)))
			.fetch()).containsExactly(VISIBLE);
	}

	@Test
	@DisplayName("R8 native 쿼리: 조건에 걸린 회원도 엔터티로 읽히고, 고친 값이 저장된다")
	void nativeQueryReadsAndWrites() {
		for (long id : List.of(DELETED, WITHDRAWN)) {
			transactionTemplate.executeWithoutResult(status -> {
				User hidden = (User)entityManager
					.createNativeQuery("select * from tb_user where id = :id for update", User.class)
					.setParameter("id", id)
					.getSingleResult();
				hidden.rename("고친이름");
			});

			assertThat(jdbcTemplate.queryForObject("select name from tb_user where id = ?", String.class, id))
				.isEqualTo("고친이름");
		}
	}

	@Test
	@DisplayName("R8 findByLoginId: 유예 중인 회원은 나오고(아이디 붙잡기), 지운 회원은 나오지 않는다")
	void loginIdIsHeldWhileWithdrawn() {
		assertThat(userRepository.findByLoginId("member_" + VISIBLE)).isPresent();
		assertThat(userRepository.findByLoginId("member_" + WITHDRAWN)).isPresent();
		assertThat(userRepository.findByLoginId("member_" + DELETED)).isEmpty();
	}

	@Test
	@DisplayName("R8 예외 조회: 유예 중인 회원만 더 나오고, 어떤 상태든 보는 조회는 모두 나온다")
	void exceptionalReads() {
		transactionTemplate.executeWithoutResult(status -> {
			assertThat(userRepository.findIncludingWithdrawn(WITHDRAWN)).isPresent();
			assertThat(userRepository.findIncludingWithdrawn(DELETED)).isEmpty();
			assertThat(userRepository.findForUpdateIncludingWithdrawn(WITHDRAWN)).isPresent();
			assertThat(userRepository.findForUpdateIncludingWithdrawn(DELETED)).isEmpty();
		});
		for (long id : List.of(VISIBLE, DELETED, WITHDRAWN)) {
			assertThat(userRepository.findInAnyState(id)).isPresent();
		}
		assertThat(userRepository.countWithdrawn(WITHDRAWN)).isEqualTo(1);
		assertThat(userRepository.countWithdrawn(VISIBLE)).isZero();
		assertThat(userRepository.countWithdrawn(DELETED)).isZero();
	}

	@Test
	@DisplayName("FR-018 회원 상태: 활동 중·탈퇴·지움·파기를 나누고, 파기한 뒤에도 탈퇴한 시각이 남는다")
	void memberStatus() {
		assertThat(memberStatusService.find(VISIBLE)).get().extracting(MemberStatusDto::state)
			.isEqualTo(MemberStatusDto.State.ACTIVE);
		assertThat(memberStatusService.find(DELETED)).get().extracting(MemberStatusDto::state)
			.isEqualTo(MemberStatusDto.State.DELETED);
		assertThat(memberStatusService.find(WITHDRAWN)).get().satisfies(status -> {
			assertThat(status.state()).isEqualTo(MemberStatusDto.State.WITHDRAWN);
			assertThat(status.withdrawnAt()).isNotNull();
		});
		assertThat(memberStatusService.isWithdrawn(WITHDRAWN)).isTrue();
		assertThat(memberStatusService.isWithdrawn(DELETED)).isFalse();
		assertThat(memberStatusService.find(999L)).isEmpty();

		jdbcTemplate.update("update tb_user set purged_at = now(6), deleted_at = now(6) where id = ?", WITHDRAWN);

		assertThat(memberStatusService.find(WITHDRAWN)).get().satisfies(status -> {
			assertThat(status.state()).isEqualTo(MemberStatusDto.State.PURGED);
			assertThat(status.withdrawnAt()).isNotNull();
			assertThat(status.purgedAt()).isNotNull();
		});
	}
}
