package com.moa.api.repository.jpa.code;

import java.util.List;
import java.util.Optional;

import com.moa.api.entity.code.CodeGroup;
import com.moa.common.repository.SoftDeleteRepository;

public interface CodeGroupRepository extends SoftDeleteRepository<CodeGroup, Long> {

	Optional<CodeGroup> findByGroupCode(String groupCode);

	boolean existsByGroupCode(String groupCode);

	List<CodeGroup> findAllByOrderByGroupCode();
}
