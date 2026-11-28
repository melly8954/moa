package com.moa.api.controller.v1.faq;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.faq.FaqApplication;
import com.moa.api.controller.v1.faq.response.FaqPublicResponse;
import com.moa.api.controller.v1.faq.response.SupportCategoryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 FAQ (specs/017-customer-center US2). 로그인 없이 연다. 화면 경로는 /support.
 */
@Tag(name = "FAQ API")
@RestController
@RequestMapping("/api/v1/faqs")
@RequiredArgsConstructor
public class ApiFaqController {

	private final FaqApplication faqApplication;

	@Operation(summary = "FAQ 목록",
		description = "켜져 있고 분류가 사용 중인 FAQ만, 순서대로 답까지 준다. keyword는 질문이나 답에 들어 있는 글자")
	@PermitAll
	@GetMapping
	public ResponseEntity<List<FaqPublicResponse>> list(
		@RequestParam(required = false) @Size(max = 50) @Nullable String categoryCode,
		@RequestParam(required = false) @Size(max = 200) @Nullable String keyword) {
		return ResponseEntity.ok(
			faqApplication.getPublicList(categoryCode, keyword).stream().map(FaqPublicResponse::from).toList());
	}

	@Operation(summary = "고객센터 분류",
		description = "지금 고를 수 있는 분류만 코드 관리의 순서대로 준다. FAQ의 분류 단추와 문의·FAQ 쓰기의 분류 고르기에 쓴다. "
			+ "공통코드 화면용 목록은 로그인이 필요해 로그인하지 않은 FAQ 화면을 위해 따로 둔다")
	@PermitAll
	@GetMapping("/categories")
	public ResponseEntity<List<SupportCategoryResponse>> categories() {
		return ResponseEntity.ok(
			faqApplication.getActiveCategories().stream().map(SupportCategoryResponse::from).toList());
	}
}
