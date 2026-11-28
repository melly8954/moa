package com.moa.api.repository.jpa.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.moa.api.entity.user.User;

/**
 * 회원 파기 (specs/018-member-withdrawal research R6, data-model "파기할 때"). 모두 native다.
 * User의 기본 조회 조건(탈퇴·지움 숨김)을 타지 않아야 하고, 게임 정보(tb_user_game_profile)는 엔터티가 없다.
 *
 * <p>
 * 파기 대상: 파기되지 않았고 시스템 계정이 아니며, 스스로 탈퇴한 지 cutoff가 지났거나 관리자가 지운 지 cutoff가 지난 회원(USER).
 * 관리자 계정(ADMIN)은 탈퇴할 수 없고, 관리자가 지운 관리자 계정은 파기하지 않는다 (FR-019).
 * </p>
 */
public interface MemberPurgeRepository extends Repository<User, Long> {

	String DUE = "purged_at is null and id <> :systemId and (withdrawn_at <= :cutoff "
		+ "or (withdrawn_at is null and deleted_at <= :cutoff and role = 'USER'))";

	/** 파기 대상 ID. 두 인덱스(purged_at, withdrawn_at)·(purged_at, deleted_at)를 타게 둘로 나눠 합친다 */
	@Query(value = "select id from tb_user where purged_at is null and id <> :systemId and withdrawn_at <= :cutoff "
		+ "union select id from tb_user where purged_at is null and id <> :systemId and withdrawn_at is null "
		+ "and deleted_at <= :cutoff and role = 'USER' order by id", nativeQuery = true)
	List<Long> findDueIds(@Param("cutoff") LocalDateTime cutoff, @Param("systemId") Long systemId);

	/** 행을 잠그고 아직 파기 대상인지 다시 본다 (복구·다른 서버와 겹침). 트랜잭션 안에서 부른다 */
	@Query(value = "select id from tb_user where id = :id and " + DUE + " for update", nativeQuery = true)
	Optional<Long> lockIfDue(@Param("id") Long id, @Param("cutoff") LocalDateTime cutoff,
		@Param("systemId") Long systemId);

	/**
	 * 회원 행의 개인정보 열을 덮는다. deleted_at·deleted_by는 비었을 때만 채운다(관리자가 지운 기록은 그대로).
	 * MariaDB는 SET을 왼쪽부터 적용하므로 deleted_by를 deleted_at보다 먼저 둔다.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = "update tb_user set login_id = concat('withdrawn_', id), password = null, name = :name, "
		+ "purged_at = :now, deleted_by = case when deleted_at is null then :systemId else deleted_by end, "
		+ "deleted_at = coalesce(deleted_at, :now), updated_at = :now, updated_by = :systemId where id = :id",
		nativeQuery = true)
	int erase(@Param("id") Long id, @Param("name") String name, @Param("now") LocalDateTime now,
		@Param("systemId") Long systemId);

	@Modifying
	@Query(value = "delete from tb_user_profile_play_time where user_id = :userId", nativeQuery = true)
	int deletePlayTimes(@Param("userId") Long userId);

	@Modifying
	@Query(value = "delete from tb_user_game_profile where user_id = :userId", nativeQuery = true)
	int deleteGameProfiles(@Param("userId") Long userId);

	@Modifying
	@Query(value = "delete from tb_user_profile where user_id = :userId", nativeQuery = true)
	int deleteProfile(@Param("userId") Long userId);

	@Modifying
	@Query(value = "delete from tb_user_social_account where user_id = :userId", nativeQuery = true)
	int deleteSocialAccounts(@Param("userId") Long userId);

	@Modifying
	@Query(value = "delete from tb_user_terms_agreement where user_id = :userId", nativeQuery = true)
	int deleteTermsAgreements(@Param("userId") Long userId);
}
