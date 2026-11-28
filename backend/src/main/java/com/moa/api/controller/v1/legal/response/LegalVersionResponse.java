package com.moa.api.controller.v1.legal.response;

import java.time.LocalDate;
import java.util.List;

import com.moa.api.dto.legal.LegalVersionDto;
import com.moa.api.dto.legal.VersionState;
import com.moa.common.legal.LegalVersion;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "문서 버전 하나 (예전·지금·예정)")
public record LegalVersionResponse(
	@Schema(description = "문서 키", example = "privacy") String document,
	@Schema(description = "버전 이름", example = "draft") String version,
	@Schema(description = "시행일 (한국 날짜)", example = "2026-11-01") LocalDate effectiveDate,
	@Schema(description = "제목", example = "moa 개인정보처리방침") String title,
	@Schema(description = "장 목록") List<LegalSectionResponse> sections,
	@Schema(description = "오늘 기준 상태") VersionState state,
	@Schema(description = "지금 시행 중인 버전 이름", example = "draft") String currentVersion) {

	public static LegalVersionResponse from(LegalVersionDto dto) {
		LegalVersion version = dto.version();
		return new LegalVersionResponse(version.document().key(), version.version(), version.effectiveDate(),
			version.title(), version.sections().stream().map(LegalSectionResponse::from).toList(), dto.state(),
			dto.currentVersion());
	}
}
