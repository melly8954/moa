package com.moa.api.repository.jpa.role;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.moa.api.entity.role.UserAccessRole;

public interface UserAccessRoleRepository extends JpaRepository<UserAccessRole, Long> {

	List<UserAccessRole> findAllByUserId(Long userId);

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
