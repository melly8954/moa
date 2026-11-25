package com.moa.api.controller.v1.admin.banner;

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

import com.moa.api.application.v1.banner.BannerApplication;
import com.moa.api.controller.v1.admin.banner.request.BannerEnabledRequest;
import com.moa.api.controller.v1.admin.banner.request.BannerIdsRequest;
import com.moa.api.controller.v1.admin.banner.request.BannerSaveRequest;
import com.moa.api.controller.v1.admin.banner.request.BannerSearchRequest;
import com.moa.api.controller.v1.admin.banner.response.BannerAdminListResponse;
import com.moa.api.controller.v1.admin.banner.response.BannerAdminResponse;
import com.moa.api.permission.BannerPermissions;
import com.moa.common.response.PageResponse;
import com.moa.common.security.permission.RequirePermission;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * 배너 관리 (specs/016-banner-popup). BANNER_MANAGE 권한. 화면 경로는 /admin/banners.
 * 사용자용 API는 두지 않는다 (FR-011).
 */
@Tag(name = "관리자 배너 API")
@RequirePermission(BannerPermissions.MANAGE)
@RestController
@RequestMapping("/api/v1/admin/banners")
@RequiredArgsConstructor
public class ApiAdminBannerController {

	private final BannerApplication bannerApplication;

	@Operation(summary = "배너 목록", description = "순서, 노출 시작이 늦은 것 순 페이징. 상태를 보내지 않으면 끝난 배너는 뺀다")
	@GetMapping
	public ResponseEntity<PageResponse<BannerAdminListResponse>> search(
		@Valid @ModelAttribute BannerSearchRequest request,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return ResponseEntity.ok(PageResponse.from(
			bannerApplication.getAdminPage(request.keyword(), request.status(), PageRequest.of(page, size)),
			BannerAdminListResponse::from));
	}

	@Operation(summary = "배너 하나", description = "없으면 R001")
	@GetMapping("/{id}")
	public ResponseEntity<BannerAdminResponse> get(@PathVariable Long id) {
		return ResponseEntity.ok(BannerAdminResponse.from(bannerApplication.get(id)));
	}

	@Operation(summary = "배너 등록",
		description = "이미지는 rich-text 이미지 올리기로 받은 주소. GIF, 없는 이미지, 끝 일시가 시작보다 앞이면 V001")
	@PostMapping
	public ResponseEntity<BannerAdminResponse> create(@Valid @RequestBody BannerSaveRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(BannerAdminResponse.from(bannerApplication.create(request.toContent())));
	}

	@Operation(summary = "배너 고치기", description = "모든 칸을 바꾼다. 빠진 이미지는 정리된다. 없으면 R001")
	@PutMapping("/{id}")
	public ResponseEntity<BannerAdminResponse> modify(@PathVariable Long id,
		@Valid @RequestBody BannerSaveRequest request) {
		return ResponseEntity.ok(BannerAdminResponse.from(bannerApplication.modify(id, request.toContent())));
	}

	@Operation(summary = "배너 지우기", description = "소프트 삭제. 이미지는 임시로 돌아가 정리된다. 없으면 R001")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		bannerApplication.delete(id);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "여러 배너 켜기·끄기", description = "하나라도 없으면 R001이고 아무것도 바꾸지 않는다")
	@PutMapping("/enabled")
	public ResponseEntity<Void> setEnabled(@Valid @RequestBody BannerEnabledRequest request) {
		bannerApplication.setEnabled(request.ids(), request.enabled());
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "여러 배너 지우기", description = "하나라도 없으면 R001이고 아무것도 지우지 않는다")
	@PostMapping("/bulk-delete")
	public ResponseEntity<Void> deleteAll(@Valid @RequestBody BannerIdsRequest request) {
		bannerApplication.deleteAll(request.ids());
		return ResponseEntity.noContent().build();
	}
}
