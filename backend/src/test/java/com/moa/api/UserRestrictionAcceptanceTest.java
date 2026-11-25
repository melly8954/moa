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

import com.moa.api.entity.user.User;
import com.moa.api.repository.jpa.user.UserRepository;
import com.moa.common.security.Role;
import com.moa.support.AcceptanceTest;
import com.moa.support.DatabaseCleaner;
import com.moa.support.TestUsers;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;

/**
 * User의 기본 조회 조건(@SQLRestriction)이 조회 종류마다 붙는지 (specs/018-member-withdrawal research R8, tasks T002).
 * 유예 중인 회원을 이 조건 하나로 숨기는 설계가 기대는 동작이다. 조건에 걸리는 회원(HIDDEN)은 어떤 JPA 조회에도 나오지 않고,
 * native 쿼리로 읽은 엔터티만 예외다 (로그인·복구·파기가 쓴다).
 */
class UserRestrictionAcceptanceTest extends AcceptanceTest {

	private static final long VISIBLE = 10L;
	private static final long HIDDEN = 11L;

	@Autowired
	private UserRepository userRepository;

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
		for (long id : List.of(VISIBLE, HIDDEN)) {
			testUsers.create(id, "member_" + id, null, Role.USER);
			jdbcTemplate.update("insert into tb_user_social_account (user_id, provider, subject, email, display_name, "
				+ "created_at) values (?, 'GOOGLE', ?, ?, null, now(6))", id, "subject-" + id, id + "@example.com");
		}
		jdbcTemplate.update("update tb_user set deleted_at = now(6), deleted_by = 1 where id = ?", HIDDEN);
	}

	@Test
	@DisplayName("R8 findById: 조건에 걸린 회원은 나오지 않는다")
	void findById() {
		assertThat(userRepository.findById(VISIBLE)).isPresent();
		assertThat(userRepository.findById(HIDDEN)).isEmpty();
	}

	@Test
	@DisplayName("R8 메서드 이름 쿼리: 조건에 걸린 회원은 나오지 않는다")
	void derivedQuery() {
		assertThat(userRepository.findByLoginId("member_" + VISIBLE)).isPresent();
		assertThat(userRepository.findByLoginId("member_" + HIDDEN)).isEmpty();
	}

	@Test
	@DisplayName("R8 QueryDSL selectFrom: 조건에 걸린 회원은 나오지 않는다")
	void selectFrom() {
		assertThat(queryFactory.select(user.id).from(user).where(user.id.in(VISIBLE, HIDDEN)).fetch())
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

		assertThat(rows).extracting(row -> row.get(userSocialAccount.userId)).containsExactly(VISIBLE, HIDDEN);
		assertThat(rows).extracting(row -> row.get(user.id)).containsExactly(VISIBLE, null);
		assertThat(rows).extracting(row -> row.get(user.name)).containsExactly("member_" + VISIBLE, null);
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
		transactionTemplate.executeWithoutResult(status -> {
			User hidden = (User)entityManager
				.createNativeQuery("select * from tb_user where id = :id for update", User.class)
				.setParameter("id", HIDDEN)
				.getSingleResult();
			hidden.rename("고친이름");
		});

		assertThat(jdbcTemplate.queryForObject("select name from tb_user where id = ?", String.class, HIDDEN))
			.isEqualTo("고친이름");
	}
}
