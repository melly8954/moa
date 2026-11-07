package com.moa.api.repository.reader.code;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.moa.api.entity.code.Code;
import com.moa.api.entity.code.CodeGroup;
import com.moa.api.repository.jpa.code.CodeGroupRepository;
import com.moa.api.repository.jpa.code.CodeRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CodeReader {

	private final CodeGroupRepository codeGroupRepository;
	private final CodeRepository codeRepository;

	public List<CodeGroup> findGroups() {
		return codeGroupRepository.findAllByOrderByGroupCode();
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

	/** 사용하는 코드만, 정렬 순서대로 */
	public List<Code> findEnabledCodes(String groupCode) {
		return codeRepository.findByGroupCodeAndEnabledTrueOrderBySortOrderAscCodeAsc(groupCode);
	}
}
