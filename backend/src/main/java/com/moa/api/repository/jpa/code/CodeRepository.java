package com.moa.api.repository.jpa.code;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moa.api.entity.code.Code;
import com.moa.common.repository.SoftDeleteRepository;

public interface CodeRepository extends SoftDeleteRepository<Code, Long> {

	List<Code> findByGroupCodeOrderBySortOrderAscCodeAsc(String groupCode);

	List<Code> findByGroupCodeAndEnabledTrueOrderBySortOrderAscCodeAsc(String groupCode);

	List<Code> findByGroupCodeInOrderBySortOrderAscCodeAsc(Collection<String> groupCodes);

	Optional<Code> findByGroupCodeAndCode(String groupCode, String code);

	boolean existsByGroupCodeAndCode(String groupCode, String code);

	/**
	 * moa: 그룹 안에서 숫자로만 된 코드의 가장 큰 값. 지운 항목도 센다 (specs/004-admin-codes FR-006, research R6).
	 * 지운 항목은 엔티티의 @SQLRestriction 밖이라 네이티브 쿼리로 본다
	 */
	@Query(value = "SELECT MAX(CAST(code AS UNSIGNED)) FROM codes WHERE group_code = :groupCode "
		+ "AND code REGEXP '^[0-9]+$'", nativeQuery = true)
	Long findMaxNumericCode(@Param("groupCode") String groupCode);

	/** moa: 그룹의 가장 큰 정렬 순서 (사용 안 함 포함, 지운 것 빼고) */
	@Query("SELECT MAX(c.sortOrder) FROM Code c WHERE c.groupCode = :groupCode")
	Integer findMaxSortOrder(@Param("groupCode") String groupCode);
}
