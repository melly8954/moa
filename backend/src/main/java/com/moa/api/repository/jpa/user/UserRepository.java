package com.moa.api.repository.jpa.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.hibernate.jpa.HibernateHints;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import com.moa.api.entity.user.User;
import com.moa.common.repository.SoftDeleteRepository;
import com.moa.common.security.Role;

import jakarta.persistence.QueryHint;

/**
 * moa 018 (specs/018-member-withdrawal research R8): JPQL·메서드 이름 쿼리는 활동 중인 회원만 본다(User.ACTIVE).
 * native 쿼리는 그 조건을 타지 않는다. 유예 중인 회원을 봐야 하는 곳(로그인, 복구, 탈퇴 확인)만 쓴다.
 */
public interface UserRepository extends SoftDeleteRepository<User, Long> {

	/** 이 로그인 아이디를 쓰는 회원. 유예 중인 회원도 나온다 (아이디는 파기할 때까지 붙잡는다, FR-012) */
	@Query(value = "select * from tb_user where login_id = :loginId and deleted_at is null", nativeQuery = true)
	Optional<User> findByLoginId(@Param("loginId") String loginId);

	/** moa 018: 유예 중인 회원도 나온다. 관리자가 지운 회원은 나오지 않는다 */
	@Query(value = "select * from tb_user where id = :id and deleted_at is null", nativeQuery = true)
	Optional<User> findIncludingWithdrawn(@Param("id") Long id);

	/** moa 018: 탈퇴·복구 전에 행을 잠근다. 유예 중인 회원도 나온다 */
	@Query(value = "select * from tb_user where id = :id and deleted_at is null for update", nativeQuery = true)
	Optional<User> findForUpdateIncludingWithdrawn(@Param("id") Long id);

	/** moa 018: 지운 회원과 파기된 회원까지 (회원 상태 조회) */
	@Query(value = "select * from tb_user where id = :id", nativeQuery = true)
	Optional<User> findInAnyState(@Param("id") Long id);

	/**
	 * moa 018: 스스로 탈퇴한 회원이면 1. 요청마다 부르므로 PK로 한 행만 본다.
	 * native 쿼리는 Hibernate가 무엇을 읽는지 몰라 부르기 전에 대기 변경을 모두 DB로 내보낸다(AUTO). 다른 기능의 트랜잭션 중간(notify 등)에서
	 * 불리므로 내보내지 않게 한다(COMMIT). withdrawn_at은 탈퇴·복구 트랜잭션만 바꾸고, 같은 트랜잭션에서 바꾼 뒤 이것을 부르는 곳은 없다
	 */
	@QueryHints(@QueryHint(name = HibernateHints.HINT_FLUSH_MODE, value = "COMMIT"))
	@Query(value = "select count(*) from tb_user where id = :id and withdrawn_at is not null", nativeQuery = true)
	long countWithdrawn(@Param("id") Long id);

	/**
	 * moa 017: 이 시각 전에 스스로 탈퇴한 회원 ID (유예 중, 파기됨). 복구한 회원은 withdrawn_at이 비어 빠진다.
	 * 탈퇴 회원은 기본 조회에 없으므로 native로 읽는다 (specs/017-customer-center FR-017·research R10)
	 */
	@Query(value = "select id from tb_user where withdrawn_at is not null and withdrawn_at <= :cutoff",
		nativeQuery = true)
	List<Long> findIdsWithdrawnBefore(@Param("cutoff") LocalDateTime cutoff);

	boolean existsByRole(Role role);
}
