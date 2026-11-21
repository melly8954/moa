package com.moa.api.controller.v1.admin.purgelog;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.purgelog.PurgeLogApplication;
import com.moa.api.controller.v1.admin.purgelog.request.PurgeLogSearchRequest;
import com.moa.api.controller.v1.admin.purgelog.response.PurgeLogListResponse;
import com.moa.api.permission.PurgeLogPermissions;
import com.moa.common.response.PageResponse;
import com.moa.common.security.permission.RequirePermission;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * 개인정보 파기 기록 조회 (moa, specs/013-purge-log US2). 보기만 한다.
 */
@Tag(name = "관리자 파기 기록 API")
@RequirePermission(PurgeLogPermissions.READ)
@RestController
@RequestMapping("/api/v1/admin/purge-logs")
@RequiredArgsConstructor
public class ApiAdminPurgeLogController {

	private final PurgeLogApplication purgeLogApplication;

	@Operation(summary = "파기 기록 목록",
		description = "최신순. 조건(target, from, to)은 모두 선택이고 함께 주면 AND. from이 to보다 늦으면 V001")
	@GetMapping
	public ResponseEntity<PageResponse<PurgeLogListResponse>> getPage(
		@Valid @ModelAttribute PurgeLogSearchRequest search,
		@Parameter(description = "페이지 (0부터)") @RequestParam(defaultValue = "0") @Min(0) int page,
		@Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return ResponseEntity.ok(PageResponse.from(
			purgeLogApplication.getPage(search.toQuery(), PageRequest.of(page, size)), PurgeLogListResponse::from));
	}
}
