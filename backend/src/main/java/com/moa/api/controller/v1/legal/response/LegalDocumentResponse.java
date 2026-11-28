package com.moa.api.controller.v1.legal.response;

import java.time.LocalDate;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.legal.LegalDocumentDto;
import com.moa.api.dto.legal.LegalVersionDto;
import com.moa.api.dto.legal.VersionState;
import com.moa.common.legal.LegalVersion;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "문서의 지금 버전 전문, 시행 예정 버전, 버전 목록")
public record LegalDocumentResponse(
	@Schema(description = "문서 키", example = "privacy") String document,
	@Schema(description = "지금 시행 중인 버전 전문") Current current,
	@Schema(description = "시행일이 가장 이른 시행 예정 버전. 없으면 null") @Nullable Upcoming upcoming,
	@Schema(description = "버전 목록. 시행일 최근순이고 예정 버전도 든다") List<Item> versions) {

	public static LegalDocumentResponse from(LegalDocumentDto dto) {
		LegalVersion current = dto.current().version();
		LegalVersionDto upcoming = dto.upcoming();
		return new LegalDocumentResponse(current.document().key(),
			new Current(current.version(), current.effectiveDate(), current.title(),
				current.sections().stream().map(LegalSectionResponse::from).toList()),
			upcoming == null ? null
				: new Upcoming(upcoming.version().version(), upcoming.version().effectiveDate()),
			dto.versions().stream()
				.map(v -> new Item(v.version().version(), v.version().effectiveDate(), v.state()))
				.toList());
	}

	@Schema(description = "지금 시행 중인 버전")
	public record Current(
		@Schema(description = "버전 이름", example = "draft") String version,
		@Schema(description = "시행일 (한국 날짜)", example = "2026-11-01") LocalDate effectiveDate,
		@Schema(description = "제목", example = "moa 개인정보처리방침") String title,
		@Schema(description = "장 목록") List<LegalSectionResponse> sections) {
	}

	@Schema(description = "시행 예정 버전")
	public record Upcoming(
		@Schema(description = "버전 이름", example = "2027-01-01") String version,
		@Schema(description = "시행일 (한국 날짜)", example = "2027-01-01") LocalDate effectiveDate) {
	}

	@Schema(description = "버전 목록의 한 줄")
	public record Item(
		@Schema(description = "버전 이름", example = "draft") String version,
		@Schema(description = "시행일 (한국 날짜)", example = "2026-11-01") LocalDate effectiveDate,
		@Schema(description = "오늘 기준 상태") VersionState state) {
	}
}
