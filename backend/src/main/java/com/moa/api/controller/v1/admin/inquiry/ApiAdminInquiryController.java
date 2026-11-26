package com.moa.api.controller.v1.admin.inquiry;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.inquiry.InquiryApplication;
import com.moa.api.controller.v1.admin.inquiry.request.InquiryAnswerRequest;
import com.moa.api.controller.v1.admin.inquiry.request.InquirySearchRequest;
import com.moa.api.controller.v1.admin.inquiry.response.InquiryAdminListResponse;
import com.moa.api.controller.v1.admin.inquiry.response.InquiryAdminResponse;
import com.moa.api.permission.InquiryPermissions;
import com.moa.common.response.PageResponse;
import com.moa.common.security.permission.RequirePermission;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * 문의 관리 (specs/017-customer-center US4). INQUIRY_MANAGE 권한. 화면 경로는 /admin/inquiries.
 * 문의를 지우거나 한꺼번에 처리하는 기능은 두지 않는다 (FR-010).
 */
@Tag(name = "관리자 1:1 문의 API")
@RequirePermission(InquiryPermissions.MANAGE)
@RestController
@RequestMapping("/api/v1/admin/inquiries")
@RequiredArgsConstructor
public class ApiAdminInquiryController {

	private final InquiryApplication inquiryApplication;

	@Operation(summary = "문의 목록", description = "쓴 최신순 페이징. 상태를 보내지 않으면 답변 대기만")
	@GetMapping
	public ResponseEntity<PageResponse<InquiryAdminListResponse>> search(
		@Valid @ModelAttribute InquirySearchRequest request,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return ResponseEntity.ok(PageResponse.from(
			inquiryApplication.getAdminPage(request.status(), request.categoryCode(), request.from(), request.to(),
				request.keyword(), PageRequest.of(page, size)),
			InquiryAdminListResponse::from));
	}

	@Operation(summary = "문의 하나", description = "없으면 R001")
	@GetMapping("/{id}")
	public ResponseEntity<InquiryAdminResponse> get(@PathVariable Long id) {
		return ResponseEntity.ok(InquiryAdminResponse.from(inquiryApplication.getForAdmin(id)));
	}

	@Operation(summary = "답변 쓰기·고치기",
		description = "처음 답변이면 답변 완료가 되고 회원에게 알림(INQUIRY_ANSWERED)이 간다. 고치면 알림은 다시 가지 않는다. 없으면 R001")
	@PutMapping("/{id}/answer")
	public ResponseEntity<Void> answer(@PathVariable Long id, @Valid @RequestBody InquiryAnswerRequest request) {
		inquiryApplication.answer(id, request.content());
		return ResponseEntity.noContent().build();
	}
}
