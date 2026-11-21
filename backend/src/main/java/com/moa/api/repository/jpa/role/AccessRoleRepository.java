package com.moa.api.repository.jpa.role;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;

import com.moa.api.entity.role.AccessRole;
import com.moa.common.repository.SoftDeleteRepository;

public interface AccessRoleRepository extends SoftDeleteRepository<AccessRole, Long> {

	List<AccessRole> findAllByOrderByIdAsc();

	List<AccessRole> findAllByIdIn(Collection<Long> ids);

	boolean existsByCode(String code);

	/** 지우지 않은 역할 중 이 코드 (moa: 최고 관리자 역할 찾기, specs/003-admin-roles FR-010) */
	Optional<AccessRole> findByCode(String code);

	/** 이 사용자의 역할들에 묶인 권한 키 (지운 역할 제외) */
	@Query("select distinct p from AccessRole r join r.permissions p"
		+ " where r.id in (select u.accessRoleId from UserAccessRole u where u.userId = :userId)")
	List<String> findPermissionsByUserId(Long userId);
}
