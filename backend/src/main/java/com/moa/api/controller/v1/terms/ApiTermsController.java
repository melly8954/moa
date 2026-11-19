package com.moa.api.controller.v1.terms;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.controller.v1.terms.response.TermsResponse;
import com.moa.common.terms.TermsProperties;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;

/**
 * 현재 약관 버전 (specs/010-social-login FR-013, research R11). 로그인 없이 부른다.
 */
@Tag(name = "약관 API", description = "가입 때 동의받을 현재 약관 버전")
@PermitAll
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/terms")
@RequiredArgsConstructor
public class ApiTermsController {

	private final TermsProperties termsProperties;

	@Operation(summary = "현재 약관 버전", description = "필수 약관(SERVICE, PRIVACY)마다 현재 버전. 문안은 프론트가 버전으로 고른다",
		responses = @ApiResponse(responseCode = "200", description = "현재 약관", useReturnTypeSchema = true))
	@GetMapping
	public ResponseEntity<TermsResponse> current() {
		return ResponseEntity.ok(TermsResponse.from(termsProperties));
	}
}
