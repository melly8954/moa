package com.moa.api.controller.v1.admin.user;

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

import com.moa.api.application.v1.user.UserAdminApplication;
import com.moa.api.controller.v1.admin.user.request.UserCreateRequest;
import com.moa.api.controller.v1.admin.user.request.UserRenameRequest;
import com.moa.api.controller.v1.admin.user.request.UserRoleChangeRequest;
import com.moa.api.controller.v1.admin.user.request.UserSearchRequest;
import com.moa.api.controller.v1.admin.user.response.TemporaryPasswordResponse;
import com.moa.api.controller.v1.admin.user.response.UserAdminResponse;
import com.moa.api.controller.v1.admin.user.response.UserCreateResponse;
import com.moa.api.permission.UserPermissions;
import com.moa.common.response.PageResponse;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.CurrentUser;
import com.moa.common.security.permission.RequirePermission;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 관리 (선택 헬퍼 user-admin). USER_MANAGE 권한. 화면 경로는 /admin/users.
 * 사용자에게 업무 역할을 주는 것은 rbac 헬퍼의 /api/v1/admin/users/{userId}/roles다.
 */
@Tag(name = "관리자 사용자 API")
@RequirePermission(UserPermissions.MANAGE)
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class ApiAdminUserController {

	private final UserAdminApplication userAdminApplication;

	@Operation(summary = "사용자 목록",
		description = "최신순 페이징. 지운 사용자와 시스템 계정은 빠진다. moa: 역할, 최고 관리자, 요청한 사람이 다룰 수 없는 이유(protection)")
	@GetMapping
	public ResponseEntity<PageResponse<UserAdminResponse>> search(@CurrentUser AuthPrincipal actor,
		@Valid @ModelAttribute UserSearchRequest request,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return ResponseEntity.ok(PageResponse.from(userAdminApplication.search(actor, request.keyword(), request.role(),
			request.locked(), PageRequest.of(page, size)), UserAdminResponse::from));
	}

	@Operation(summary = "사용자 하나", description = "없으면 R001")
	@GetMapping("/{id}")
	public ResponseEntity<UserAdminResponse> get(@CurrentUser AuthPrincipal actor, @PathVariable Long id) {
		return ResponseEntity.ok(UserAdminResponse.from(userAdminApplication.get(actor, id)));
	}

	@Operation(summary = "사용자 만들기",
		description = "임시 비밀번호로 만들고 응답에 한 번만 보인다. 아이디 형식 V001, 중복 R002. moa: 계정 종류 ADMIN(관리자 계정)도 사용자 관리 권한으로 만든다")
	@PostMapping
	public ResponseEntity<UserCreateResponse> create(@CurrentUser AuthPrincipal actor,
		@Valid @RequestBody UserCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(UserCreateResponse.from(
			userAdminApplication.create(actor, request.loginId(), request.name(), request.role())));
	}

	@Operation(summary = "사용자 정보 바꾸기", description = "이름. 없으면 R001, 다룰 수 없는 계정(최고 관리자 계정, 나보다 권한이 많은 관리자 계정) A005")
	@PutMapping("/{id}")
	public ResponseEntity<UserAdminResponse> rename(@CurrentUser AuthPrincipal actor, @PathVariable Long id,
		@Valid @RequestBody UserRenameRequest request) {
		return ResponseEntity.ok(UserAdminResponse.from(userAdminApplication.rename(actor, id, request.name())));
	}

	@Operation(summary = "계정 종류 바꾸기",
		description = "moa: 회원(USER)·관리자 계정(ADMIN). 자기 자신 B001, "
			+ "다룰 수 없는 계정(최고 관리자 계정, 나보다 권한이 많은 관리자 계정) A005, 최고 관리자가 0명이 되면 B001. "
			+ "회원으로 바꾸면 역할을 모두 빼고, 바뀌면 모든 기기에서 로그아웃시킨다")
	@PutMapping("/{id}/role")
	public ResponseEntity<UserAdminResponse> changeRole(@CurrentUser AuthPrincipal actor, @PathVariable Long id,
		@Valid @RequestBody UserRoleChangeRequest request) {
		return ResponseEntity.ok(UserAdminResponse.from(userAdminApplication.changeRole(actor, id, request.role())));
	}

	@Operation(summary = "잠그기",
		description = "로그인·토큰 재발급을 막고 모든 기기에서 로그아웃시킨다. 자기 자신 B001, "
			+ "다룰 수 없는 계정(최고 관리자 계정, 나보다 권한이 많은 관리자 계정) A005, 최고 관리자가 0명이 되면 B001")
	@PutMapping("/{id}/lock")
	public ResponseEntity<Void> lock(@CurrentUser AuthPrincipal actor, @PathVariable Long id) {
		userAdminApplication.lock(actor, id);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "잠금 풀기", description = "다룰 수 없는 계정(최고 관리자 계정, 나보다 권한이 많은 관리자 계정) A005")
	@DeleteMapping("/{id}/lock")
	public ResponseEntity<Void> unlock(@CurrentUser AuthPrincipal actor, @PathVariable Long id) {
		userAdminApplication.unlock(actor, id);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "비밀번호 초기화",
		description = "임시 비밀번호로 바꾸고 응답에 한 번만 보인다. 모든 기기에서 로그아웃시킨다. 비밀번호 없는 계정(소셜) B001, "
			+ "다룰 수 없는 계정(최고 관리자 계정, 나보다 권한이 많은 관리자 계정) A005")
	@PostMapping("/{id}/password-reset")
	public ResponseEntity<TemporaryPasswordResponse> resetPassword(@CurrentUser AuthPrincipal actor,
		@PathVariable Long id) {
		return ResponseEntity.ok(new TemporaryPasswordResponse(userAdminApplication.resetPassword(actor, id)));
	}

	@Operation(summary = "사용자 지우기",
		description = "소프트 삭제, 모든 기기에서 로그아웃. 같은 아이디로 다시 만들 수 있다. 자기 자신 B001, "
			+ "다룰 수 없는 계정(최고 관리자 계정, 나보다 권한이 많은 관리자 계정) A005, 최고 관리자가 0명이 되면 B001. 지운 계정의 역할도 뺀다")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@CurrentUser AuthPrincipal actor, @PathVariable Long id) {
		userAdminApplication.delete(actor, id);
		return ResponseEntity.noContent().build();
	}
}
