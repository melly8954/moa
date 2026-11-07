package com.moa.api.repository.jpa.code;

import java.util.List;
import java.util.Optional;

import com.moa.api.entity.code.Code;
import com.moa.common.repository.SoftDeleteRepository;

public interface CodeRepository extends SoftDeleteRepository<Code, Long> {

	List<Code> findByGroupCodeOrderBySortOrderAscCodeAsc(String groupCode);

	List<Code> findByGroupCodeAndEnabledTrueOrderBySortOrderAscCodeAsc(String groupCode);

	Optional<Code> findByGroupCodeAndCode(String groupCode, String code);

	boolean existsByGroupCodeAndCode(String groupCode, String code);
}
