package com.moa.api.controller.v1.admin.faq;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.faq.FaqApplication;
import com.moa.api.controller.v1.admin.faq.request.FaqEnabledRequest;
import com.moa.api.controller.v1.admin.faq.request.FaqIdsRequest;
import com.moa.api.controller.v1.admin.faq.request.FaqSaveRequest;
import com.moa.api.controller.v1.admin.faq.request.FaqSearchRequest;
import com.moa.api.controller.v1.admin.faq.response.FaqAdminListResponse;
import com.moa.api.controller.v1.admin.faq.response.FaqAdminResponse;
import com.moa.api.permission.FaqPermissions;
import com.moa.common.security.permission.RequirePermission;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * FAQ 관리 (specs/017-customer-center US1). FAQ_MANAGE 권한. 화면 경로는 /admin/faqs.
 * 상한(200개)이 있는 구성 목록이라 쪽을 나누지 않는다 (FR-015).
 */
@Tag(name = "관리자 FAQ API")
@RequirePermission(FaqPermissions.MANAGE)
@RestController
@RequestMapping("/api/v1/admin/faqs")
@RequiredArgsConstructor
public class ApiAdminFaqController {

	private final FaqApplication faqApplication;

	@Operation(summary = "FAQ 목록", description = "전체(쪽 없음). 순서, 나중에 등록한 것 순")
	@GetMapping
	public ResponseEntity<List<FaqAdminListResponse>> search(@Valid @ModelAttribute FaqSearchRequest request) {
		return ResponseEntity.ok(faqApplication.getAdminList(request.categoryCode(), request.enabled(),
			request.keyword()).stream().map(FaqAdminListResponse::from).toList());
	}

	@Operation(summary = "FAQ 하나", description = "없으면 R001")
	@GetMapping("/{id}")
	public ResponseEntity<FaqAdminResponse> get(@PathVariable Long id) {
		return ResponseEntity.ok(FaqAdminResponse.from(faqApplication.get(id)));
	}

	@Operation(summary = "FAQ 등록",
		description = "답은 걸러 저장한다. 200개면 B301, 고를 수 없는 분류면 V001 (칸 categoryCode)")
	@PostMapping
	public ResponseEntity<FaqAdminResponse> create(@Valid @RequestBody FaqSaveRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(FaqAdminResponse.from(faqApplication.create(request.toContent())));
	}

	@Operation(summary = "FAQ 고치기", description = "모든 칸을 바꾼다. 없으면 R001, 분류를 바꿀 때 고를 수 없는 분류면 V001")
	@PutMapping("/{id}")
	public ResponseEntity<FaqAdminResponse> modify(@PathVariable Long id, @Valid @RequestBody FaqSaveRequest request) {
		return ResponseEntity.ok(FaqAdminResponse.from(faqApplication.modify(id, request.toContent())));
	}

	@Operation(summary = "FAQ 지우기", description = "소프트 삭제. 답 이미지는 임시로 돌아가 정리된다. 없으면 R001")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		faqApplication.delete(id);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "여러 FAQ 켜기·끄기", description = "하나라도 없으면 R001이고 아무것도 바꾸지 않는다")
	@PutMapping("/enabled")
	public ResponseEntity<Void> setEnabled(@Valid @RequestBody FaqEnabledRequest request) {
		faqApplication.setEnabled(request.ids(), request.enabled());
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "여러 FAQ 지우기", description = "하나라도 없으면 R001이고 아무것도 지우지 않는다")
	@PostMapping("/bulk-delete")
	public ResponseEntity<Void> deleteAll(@Valid @RequestBody FaqIdsRequest request) {
		faqApplication.deleteAll(request.ids());
		return ResponseEntity.noContent().build();
	}
}
