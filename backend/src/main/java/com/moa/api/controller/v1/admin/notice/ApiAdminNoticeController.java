package com.moa.api.controller.v1.admin.notice;

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

import com.moa.api.application.v1.notice.NoticeApplication;
import com.moa.api.controller.v1.admin.notice.request.NoticeCreateRequest;
import com.moa.api.controller.v1.admin.notice.request.NoticeModifyRequest;
import com.moa.api.controller.v1.admin.notice.request.NoticePinRequest;
import com.moa.api.controller.v1.admin.notice.request.NoticeSearchRequest;
import com.moa.api.controller.v1.admin.notice.response.NoticeAdminListResponse;
import com.moa.api.controller.v1.admin.notice.response.NoticeAdminResponse;
import com.moa.api.permission.NoticePermissions;
import com.moa.common.response.PageResponse;
import com.moa.common.security.permission.RequirePermission;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * 공지 관리 (specs/006-notices). NOTICE_MANAGE 권한. 화면 경로는 /admin/notices.
 */
@Tag(name = "관리자 공지 API")
@RequirePermission(NoticePermissions.MANAGE)
@RestController
@RequestMapping("/api/v1/admin/notices")
@RequiredArgsConstructor
public class ApiAdminNoticeController {

	private final NoticeApplication noticeApplication;

	@Operation(summary = "공지 목록", description = "임시 저장 포함, 고친 최신순 페이징")
	@GetMapping
	public ResponseEntity<PageResponse<NoticeAdminListResponse>> search(
		@Valid @ModelAttribute NoticeSearchRequest request,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return ResponseEntity.ok(PageResponse.from(
			noticeApplication.getAdminPage(request.keyword(), request.status(), PageRequest.of(page, size)),
			NoticeAdminListResponse::from));
	}

	@Operation(summary = "공지 하나", description = "없으면 R001")
	@GetMapping("/{id}")
	public ResponseEntity<NoticeAdminResponse> get(@PathVariable Long id) {
		return ResponseEntity.ok(NoticeAdminResponse.from(noticeApplication.get(id)));
	}

	@Operation(summary = "공지 만들기", description = "publish=true면 바로 게시, 아니면 임시 저장. 본문은 걸러 저장한다")
	@PostMapping
	public ResponseEntity<NoticeAdminResponse> create(@Valid @RequestBody NoticeCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(NoticeAdminResponse.from(noticeApplication.create(
			request.title(), request.content(), request.pinned(), request.publish())));
	}

	@Operation(summary = "공지 고치기", description = "제목과 본문. 상태와 게시 시각은 그대로다. 없으면 R001")
	@PutMapping("/{id}")
	public ResponseEntity<NoticeAdminResponse> modify(@PathVariable Long id,
		@Valid @RequestBody NoticeModifyRequest request) {
		return ResponseEntity.ok(
			NoticeAdminResponse.from(noticeApplication.modify(id, request.title(), request.content())));
	}

	@Operation(summary = "공지 게시", description = "임시 저장 공지를 게시한다. 이미 게시했으면 B001, 없으면 R001")
	@PostMapping("/{id}/publish")
	public ResponseEntity<NoticeAdminResponse> publish(@PathVariable Long id) {
		return ResponseEntity.ok(NoticeAdminResponse.from(noticeApplication.publish(id)));
	}

	@Operation(summary = "공지 고정·풀기", description = "없으면 R001")
	@PutMapping("/{id}/pin")
	public ResponseEntity<NoticeAdminResponse> pin(@PathVariable Long id,
		@Valid @RequestBody NoticePinRequest request) {
		return ResponseEntity.ok(NoticeAdminResponse.from(noticeApplication.pin(id, request.pinned())));
	}

	@Operation(summary = "공지 지우기", description = "소프트 삭제. 본문 이미지는 임시로 돌아가 정리된다. 없으면 R001")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		noticeApplication.delete(id);
		return ResponseEntity.noContent().build();
	}
}
