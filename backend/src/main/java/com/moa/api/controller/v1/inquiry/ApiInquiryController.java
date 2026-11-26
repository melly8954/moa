package com.moa.api.controller.v1.inquiry;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.inquiry.InquiryApplication;
import com.moa.api.controller.v1.inquiry.request.InquiryCreateRequest;
import com.moa.api.controller.v1.inquiry.response.MyInquiryListResponse;
import com.moa.api.controller.v1.inquiry.response.MyInquiryResponse;
import com.moa.common.response.PageResponse;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * 회원 1:1 문의 (specs/017-customer-center US3). 로그인 필요. 관리자·운영자 계정은 A005 (FR-011).
 * 남의 문의는 없는 문의로 본다 (FR-010).
 */
@Tag(name = "1:1 문의 API")
@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
public class ApiInquiryController {

	private final InquiryApplication inquiryApplication;

	@Operation(summary = "내 문의 목록", description = "쓴 최신순 페이징")
	@GetMapping("/me")
	public ResponseEntity<PageResponse<MyInquiryListResponse>> getMyPage(@CurrentUser AuthPrincipal user,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return ResponseEntity.ok(PageResponse.from(
			inquiryApplication.getMyPage(user.userId(), PageRequest.of(page, size)), MyInquiryListResponse::from));
	}

	@Operation(summary = "문의 쓰기",
		description = "첨부는 본문 이미지 올리기로 받은 주소, 다섯 장까지. 고를 수 없는 분류나 올리지 않은 이미지면 V001")
	@PostMapping
	public ResponseEntity<MyInquiryResponse> create(@CurrentUser AuthPrincipal user,
		@Valid @RequestBody InquiryCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(MyInquiryResponse.from(inquiryApplication.create(user.userId(), request.toContent())));
	}

	@Operation(summary = "내 문의 하나", description = "없거나 남의 문의면 R001")
	@GetMapping("/{id}")
	public ResponseEntity<MyInquiryResponse> get(@CurrentUser AuthPrincipal user, @PathVariable Long id) {
		return ResponseEntity.ok(MyInquiryResponse.from(inquiryApplication.getMine(user.userId(), id)));
	}

	@Operation(summary = "내 문의 지우기", description = "답변 대기만 지운다(답변 완료면 B001). 없거나 남의 문의면 R001")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@CurrentUser AuthPrincipal user, @PathVariable Long id) {
		inquiryApplication.deleteMine(user.userId(), id);
		return ResponseEntity.noContent().build();
	}
}
