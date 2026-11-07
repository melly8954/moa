package com.moa.api.repository.writer.code;

import org.springframework.stereotype.Component;

import com.moa.api.entity.code.Code;
import com.moa.api.entity.code.CodeGroup;
import com.moa.api.repository.jpa.code.CodeGroupRepository;
import com.moa.api.repository.jpa.code.CodeRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CodeWriter {

	private final CodeGroupRepository codeGroupRepository;
	private final CodeRepository codeRepository;

	public CodeGroup saveGroup(CodeGroup group) {
		return codeGroupRepository.save(group);
	}

	public Code save(Code code) {
		return codeRepository.save(code);
	}
}
