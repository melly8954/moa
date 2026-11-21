package com.moa.api.repository.reader.code;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.moa.api.entity.code.Code;
import com.moa.api.entity.code.CodeGroup;
import com.moa.api.repository.jpa.code.CodeGroupRepository;
import com.moa.api.repository.jpa.code.CodeRepository;

import lombok.RequiredArgsConstructor;

/**
 * 공통코드 그룹과 항목 (moa: 그룹은 DB의 code_groups, specs/004-admin-codes FR-004)
 */
@Component
@RequiredArgsConstructor
public class CodeReader {

	private final CodeGroupRepository codeGroupRepository;
	private final CodeRepository codeRepository;

	/** moa: 그룹 순서, 같으면 만든 순서 (FR-005) */
	public List<CodeGroup> findGroups() {
		return codeGroupRepository.findAllByOrderBySortOrderAscIdAsc();
	}

	public Optional<CodeGroup> findGroup(String groupCode) {
		return codeGroupRepository.findByGroupCode(groupCode);
	}

	public boolean existsGroup(String groupCode) {
		return codeGroupRepository.existsByGroupCode(groupCode);
	}

	public boolean existsCode(String groupCode, String code) {
		return codeRepository.existsByGroupCodeAndCode(groupCode, code);
	}

	public Optional<Code> findCode(String groupCode, String code) {
		return codeRepository.findByGroupCodeAndCode(groupCode, code);
	}

	/** 사용 안 함 포함, 정렬 순서대로 */
	public List<Code> findCodes(String groupCode) {
		return codeRepository.findByGroupCodeOrderBySortOrderAscCodeAsc(groupCode);
	}

	/** moa: 여러 그룹의 항목, 사용 안 함 포함, 정렬 순서대로 (트리 표) */
	public List<Code> findCodes(Collection<String> groupCodes) {
		return groupCodes.isEmpty() ? List.of()
			: codeRepository.findByGroupCodeInOrderBySortOrderAscCodeAsc(groupCodes);
	}

	/** 사용하는 코드만, 정렬 순서대로 */
	public List<Code> findEnabledCodes(String groupCode) {
		return codeRepository.findByGroupCodeAndEnabledTrueOrderBySortOrderAscCodeAsc(groupCode);
	}

	/** moa: 그룹의 가장 큰 정렬 순서. 항목이 없으면 -1 */
	public int findMaxSortOrder(String groupCode) {
		Integer max = codeRepository.findMaxSortOrder(groupCode);
		return max == null ? -1 : max;
	}
}
