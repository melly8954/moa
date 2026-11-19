package com.moa.api.repository.writer.code;

import org.springframework.stereotype.Component;

import com.moa.api.entity.code.Code;
import com.moa.api.repository.jpa.code.CodeRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CodeWriter {

	private final CodeRepository codeRepository;

	public Code save(Code code) {
		return codeRepository.save(code);
	}
}
