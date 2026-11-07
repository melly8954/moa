package com.moa.api.controller.v1.admin.loginhistory;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.loginhistory.LoginHistoryApplication;
import com.moa.api.controller.v1.admin.loginhistory.request.LoginHistorySearchRequest;
import com.moa.api.controller.v1.admin.loginhistory.response.LoginHistoryListResponse;
import com.moa.api.permission.LoginHistoryPermissions;
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
 * 접속 이력 조회 (선택 헬퍼 login-history). 관리자만.
 */
@Tag(name = "관리자 접속 이력 API")
@RequirePermission(LoginHistoryPermissions.READ)
@RestController
@RequestMapping("/api/v1/admin/login-histories")
@RequiredArgsConstructor
public class ApiAdminLoginHistoryController {

	private final LoginHistoryApplication loginHistoryApplication;

	@Operation(summary = "접속 이력 목록",
		description = "최신순. 조건(userId, loginId, success, from, to)은 모두 선택이고 함께 주면 AND. from이 to보다 늦으면 V001")
	@GetMapping
	public ResponseEntity<PageResponse<LoginHistoryListResponse>> getPage(
		@Valid @ModelAttribute LoginHistorySearchRequest search,
		@Parameter(description = "페이지 (0부터)") @RequestParam(defaultValue = "0") @Min(0) int page,
		@Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return ResponseEntity.ok(PageResponse.from(
			loginHistoryApplication.getPage(search.toQuery(), PageRequest.of(page, size)),
			LoginHistoryListResponse::from));
	}
}
