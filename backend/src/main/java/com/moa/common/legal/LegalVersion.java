package com.moa.common.legal;

import java.time.LocalDate;
import java.util.List;

/**
 * 문서의 버전 하나. 문안 파일 한 쌍(json + html)에서 읽는다 (specs/012-terms-privacy data-model).
 *
 * @param effectiveDate 시행일 (한국 날짜)
 */
public record LegalVersion(LegalDocument document, String version, LocalDate effectiveDate, String title,
	List<LegalSection> sections) {
}
