package com.moa.api.controller.v1.legal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.legal.LegalApplication;
import com.moa.api.controller.v1.legal.response.LegalDocumentResponse;
import com.moa.api.controller.v1.legal.response.LegalVersionResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;

/**
 * 이용약관·개인정보처리방침 (specs/012-terms-privacy FR-001·FR-002·FR-006, research R4). 로그인 없이 연다.
 * 문서 키는 문자열로 받는다. enum으로 받으면 없는 키가 400 V001이 되어서, 서비스가 404 R001로 바꾼다.
 */
@Tag(name = "공개 문서 API", description = "이용약관·개인정보처리방침의 지금 버전, 예전·예정 버전")
@PermitAll
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/legal")
@RequiredArgsConstructor
public class ApiLegalController {

	private final LegalApplication legalApplication;

	@Operation(summary = "지금 시행 중인 문서",
		description = "지금 버전 전문, 시행일이 가장 이른 예정 버전, 버전 목록. 지금 버전은 요청 때의 한국 날짜로 고른다",
		responses = {
			@ApiResponse(responseCode = "200", description = "문서", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "없는 문서 (R001)")})
	@GetMapping("/{document}")
	public ResponseEntity<LegalDocumentResponse> document(
		@Parameter(description = "문서 키 (terms, privacy)", example = "privacy") @PathVariable String document) {
		return ResponseEntity.ok(LegalDocumentResponse.from(legalApplication.getDocument(document)));
	}

	@Operation(summary = "문서 버전 하나", description = "예전·지금·예정 버전 전문과 상태, 지금 시행 중인 버전 이름",
		responses = {
			@ApiResponse(responseCode = "200", description = "버전", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "없는 문서나 버전 (R001)")})
	@GetMapping("/{document}/versions/{version}")
	public ResponseEntity<LegalVersionResponse> version(
		@Parameter(description = "문서 키 (terms, privacy)", example = "privacy") @PathVariable String document,
		@Parameter(description = "버전 이름", example = "draft") @PathVariable String version) {
		return ResponseEntity.ok(LegalVersionResponse.from(legalApplication.getVersion(document, version)));
	}
}
