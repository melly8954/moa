package com.moa.api.dto.legal;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * 문서의 지금 버전, 가장 가까운 예정 버전, 버전 목록 (시행일 최근순, 예정 포함).
 */
public record LegalDocumentDto(LegalVersionDto current, @Nullable LegalVersionDto upcoming,
	List<LegalVersionDto> versions) {
}
