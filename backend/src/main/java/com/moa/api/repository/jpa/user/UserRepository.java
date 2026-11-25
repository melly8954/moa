package com.moa.api.repository.jpa.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moa.api.entity.user.User;
import com.moa.common.repository.SoftDeleteRepository;
import com.moa.common.security.Role;

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

	/** moa 018: 스스로 탈퇴한 회원이면 1. 요청마다 부르므로 PK로 한 행만 본다 */
	@Query(value = "select count(*) from tb_user where id = :id and withdrawn_at is not null", nativeQuery = true)
	long countWithdrawn(@Param("id") Long id);

	boolean existsByRole(Role role);
}
