package com.moa.api.controller.v1.admin.popup;

import org.springframework.data.domain.PageRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.popup.PopupApplication;
import com.moa.api.controller.v1.admin.popup.request.PopupEnabledRequest;
import com.moa.api.controller.v1.admin.popup.request.PopupIdsRequest;
import com.moa.api.controller.v1.admin.popup.request.PopupSaveRequest;
import com.moa.api.controller.v1.admin.popup.request.PopupSearchRequest;
import com.moa.api.controller.v1.admin.popup.response.PopupAdminListResponse;
import com.moa.api.controller.v1.admin.popup.response.PopupAdminResponse;
import com.moa.api.permission.PopupPermissions;
import com.moa.common.response.PageResponse;
import com.moa.common.security.permission.RequirePermission;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * 팝업 관리 (specs/016-banner-popup). POPUP_MANAGE 권한. 화면 경로는 /admin/popups.
 * 사용자용 API는 두지 않는다 (FR-011).
 */
@Tag(name = "관리자 팝업 API")
@RequirePermission(PopupPermissions.MANAGE)
@RestController
@RequestMapping("/api/v1/admin/popups")
@RequiredArgsConstructor
public class ApiAdminPopupController {

	private final PopupApplication popupApplication;

	@Operation(summary = "팝업 목록", description = "순서, 노출 시작이 늦은 것 순 페이징. 상태를 보내지 않으면 끝난 팝업은 뺀다")
	@GetMapping
	public ResponseEntity<PageResponse<PopupAdminListResponse>> search(
		@Valid @ModelAttribute PopupSearchRequest request,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return ResponseEntity.ok(PageResponse.from(
			popupApplication.getAdminPage(request.keyword(), request.status(), PageRequest.of(page, size)),
			PopupAdminListResponse::from));
	}

	@Operation(summary = "팝업 하나", description = "없으면 R001")
	@GetMapping("/{id}")
	public ResponseEntity<PopupAdminResponse> get(@PathVariable Long id) {
		return ResponseEntity.ok(PopupAdminResponse.from(popupApplication.get(id)));
	}

	@Operation(summary = "팝업 등록", description = "내용은 걸러 저장한다. 끝 일시가 시작보다 앞이면 V001")
	@PostMapping
	public ResponseEntity<PopupAdminResponse> create(@Valid @RequestBody PopupSaveRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(PopupAdminResponse.from(popupApplication.create(request.toContent())));
	}

	@Operation(summary = "팝업 고치기", description = "모든 칸을 바꾼다. 없으면 R001")
	@PutMapping("/{id}")
	public ResponseEntity<PopupAdminResponse> modify(@PathVariable Long id,
		@Valid @RequestBody PopupSaveRequest request) {
		return ResponseEntity.ok(PopupAdminResponse.from(popupApplication.modify(id, request.toContent())));
	}

	@Operation(summary = "팝업 지우기", description = "소프트 삭제. 본문 이미지는 임시로 돌아가 정리된다. 없으면 R001")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		popupApplication.delete(id);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "여러 팝업 켜기·끄기", description = "하나라도 없으면 R001이고 아무것도 바꾸지 않는다")
	@PutMapping("/enabled")
	public ResponseEntity<Void> setEnabled(@Valid @RequestBody PopupEnabledRequest request) {
		popupApplication.setEnabled(request.ids(), request.enabled());
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "여러 팝업 지우기", description = "하나라도 없으면 R001이고 아무것도 지우지 않는다")
	@PostMapping("/bulk-delete")
	public ResponseEntity<Void> deleteAll(@Valid @RequestBody PopupIdsRequest request) {
		popupApplication.deleteAll(request.ids());
		return ResponseEntity.noContent().build();
	}
}
