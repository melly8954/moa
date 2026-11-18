package com.moa.api.repository.jpa.code;

import java.util.List;
import java.util.Optional;

import com.moa.api.entity.code.CodeGroup;
import com.moa.common.repository.SoftDeleteRepository;

public interface CodeGroupRepository extends SoftDeleteRepository<CodeGroup, Long> {

	Optional<CodeGroup> findByGroupCode(String groupCode);

	boolean existsByGroupCode(String groupCode);

	/** moa: 만든 순서 (specs/004-admin-codes FR-005) */
	List<CodeGroup> findAllByOrderByIdAsc();
}
