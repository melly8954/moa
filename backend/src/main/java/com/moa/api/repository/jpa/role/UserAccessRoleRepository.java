package com.moa.api.repository.jpa.role;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.moa.api.entity.role.UserAccessRole;

public interface UserAccessRoleRepository extends JpaRepository<UserAccessRole, Long> {

	List<UserAccessRole> findAllByUserId(Long userId);

	List<UserAccessRole> findAllByUserIdIn(Collection<Long> userIds);

	boolean existsByUserIdAndAccessRoleId(Long userId, Long accessRoleId);

	/** moa: 이 역할의 구성원 수 (최고 관리자가 한 명은 남는지, specs/003-admin-roles FR-010) */
	long countByAccessRoleId(Long accessRoleId);

	/** moa: 이 사용자들 중 이 역할을 가진 행 */
	List<UserAccessRole> findAllByAccessRoleIdAndUserIdIn(Long accessRoleId, Collection<Long> userIds);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from UserAccessRole u where u.userId = :userId and u.accessRoleId = :accessRoleId")
	int deleteByUserIdAndAccessRoleId(Long userId, Long accessRoleId);

	/** 역할별 사용자 수: [accessRoleId, count] */
	@Query("select u.accessRoleId, count(u) from UserAccessRole u group by u.accessRoleId")
	List<Object[]> countUsersByRole();

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from UserAccessRole u where u.userId = :userId")
	int deleteAllByUserId(Long userId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from UserAccessRole u where u.accessRoleId = :accessRoleId")
	int deleteAllByAccessRoleId(Long accessRoleId);
}
