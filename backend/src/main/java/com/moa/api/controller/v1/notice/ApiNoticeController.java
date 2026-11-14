package com.moa.api.controller.v1.notice;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.notice.NoticeApplication;
import com.moa.api.controller.v1.notice.response.NoticeListResponse;
import com.moa.api.controller.v1.notice.response.NoticeNewResponse;
import com.moa.api.controller.v1.notice.response.NoticeResponse;
import com.moa.common.response.PageResponse;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 공지 (specs/006-notices). 목록·상세는 로그인 없이 연다 (FR-006).
 * 새 공지 여부와 확인 기록은 로그인한 사용자 누구나 (역할·권한은 묻지 않는다).
 */
@Tag(name = "공지 API")
@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
public class ApiNoticeController {

	private final NoticeApplication noticeApplication;

	@Operation(summary = "공지 목록", description = "게시한 공지만. 고정 먼저, 그다음 게시 최신순")
	@PermitAll
	@SecurityRequirements
	@GetMapping
	public ResponseEntity<PageResponse<NoticeListResponse>> getPage(
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return ResponseEntity.ok(PageResponse.from(noticeApplication.getPublishedPage(PageRequest.of(page, size)),
			NoticeListResponse::from));
	}

	@Operation(summary = "새 공지 여부", description = "공지 목록을 마지막으로 연 뒤 게시된 공지가 있나. 연 적이 없으면 게시한 공지가 하나라도 있나")
	@GetMapping("/new")
	public ResponseEntity<NoticeNewResponse> hasNew(@CurrentUser AuthPrincipal user) {
		return ResponseEntity.ok(new NoticeNewResponse(noticeApplication.hasNew(user.userId())));
	}

	@Operation(summary = "공지 목록 확인", description = "지금(서버 시각)을 공지 목록을 마지막으로 연 시각으로 기록한다")
	@PutMapping("/seen")
	public ResponseEntity<Void> markSeen(@CurrentUser AuthPrincipal user) {
		noticeApplication.markSeen(user.userId());
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "공지 하나", description = "게시한 공지만. 임시 저장·지운 공지·없는 공지는 R001")
	@PermitAll
	@SecurityRequirements
	@GetMapping("/{id}")
	public ResponseEntity<NoticeResponse> get(@PathVariable Long id) {
		return ResponseEntity.ok(NoticeResponse.from(noticeApplication.getPublished(id)));
	}
}
