package com.moa.api.controller.v1.admin.role;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.role.AccessRoleApplication;
import com.moa.api.controller.v1.admin.role.request.RoleCreateRequest;
import com.moa.api.controller.v1.admin.role.request.RoleIdsRequest;
import com.moa.api.controller.v1.admin.role.request.RoleMembersRequest;
import com.moa.api.controller.v1.admin.role.request.RoleModifyRequest;
import com.moa.api.controller.v1.admin.role.request.UserRoleReplaceRequest;
import com.moa.api.controller.v1.admin.role.response.PermissionResponse;
import com.moa.api.controller.v1.admin.role.response.PermissionTreeResponse;
import com.moa.api.controller.v1.admin.role.response.RoleMemberResponse;
import com.moa.api.controller.v1.admin.role.response.RoleResponse;
import com.moa.api.permission.RolePermissions;
import com.moa.common.response.PageResponse;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.CurrentUser;
import com.moa.common.security.permission.RequirePermission;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

/**
 * 역할·권한 관리 (선택 헬퍼 rbac). ROLE_MANAGE 권한. 화면 경로는 /admin/roles (moa 화면 이름 "권한 관리").
 */
@Tag(name = "관리자 역할 API")
@RequirePermission(RolePermissions.MANAGE)
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class ApiAdminRoleController {

	private final AccessRoleApplication accessRoleApplication;

	@Operation(summary = "권한 목록", description = "코드에 등록된 권한 전부. 역할에 묶을 수 있는 것이다")
	@GetMapping("/permissions")
	public ResponseEntity<List<PermissionResponse>> getPermissions() {
		return ResponseEntity
			.ok(accessRoleApplication.getPermissions().stream().map(PermissionResponse::from).toList());
	}

	@Operation(summary = "권한 트리",
		description = "moa: 역할 창에서 권한을 고르는 관리 메뉴 트리(꺼진 메뉴 포함)와 메뉴에 없는 관리 연결 화면 (specs/003-admin-roles FR-004)")
	@GetMapping("/roles/permission-tree")
	public ResponseEntity<PermissionTreeResponse> getPermissionTree() {
		return ResponseEntity.ok(PermissionTreeResponse.from(accessRoleApplication.getPermissionTree()));
	}

	@Operation(summary = "역할 목록",
		description = "만든 순서대로. moa: 최고 관리자 역할이 맨 앞, "
			+ "요청한 사람이 다룰 수 있는지(grantable·blockReason) (specs/003-admin-roles R18)")
	@GetMapping("/roles")
	public ResponseEntity<List<RoleResponse>> getRoles(@CurrentUser AuthPrincipal actor) {
		return ResponseEntity.ok(accessRoleApplication.getRoles(actor).stream().map(RoleResponse::from).toList());
	}

	@Operation(summary = "역할 하나", description = "없으면 R001")
	@GetMapping("/roles/{id}")
	public ResponseEntity<RoleResponse> getRole(@CurrentUser AuthPrincipal actor, @PathVariable Long id) {
		return ResponseEntity.ok(RoleResponse.from(accessRoleApplication.getRole(actor, id)));
	}

	@Operation(summary = "역할 만들기", description = "코드 형식·등록되지 않은 권한 V001, 내가 가지지 않은 권한 A005, 코드 중복 R002")
	@PostMapping("/roles")
	public ResponseEntity<RoleResponse> create(@CurrentUser AuthPrincipal actor,
		@Valid @RequestBody RoleCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(RoleResponse.from(accessRoleApplication.create(actor, request.toCommand())));
	}

	@Operation(summary = "역할 바꾸기",
		description = "이름·설명·권한을 바꾼다 (코드는 그대로). 없으면 R001, 등록되지 않은 권한 V001, 다룰 수 없는 역할·권한 A005. "
			+ "moa: 최고 관리자 역할은 B001")
	@PutMapping("/roles/{id}")
	public ResponseEntity<RoleResponse> modify(@CurrentUser AuthPrincipal actor, @PathVariable Long id,
		@Valid @RequestBody RoleModifyRequest request) {
		return ResponseEntity.ok(RoleResponse.from(accessRoleApplication.modify(actor, request.toCommand(id))));
	}

	@Operation(summary = "한꺼번에 지우기",
		description = "moa (specs/003-admin-roles FR-005): 한 트랜잭션. 없는 역할 R001, 다룰 수 없는 역할 A005, 최고 관리자 역할 B001이 "
			+ "하나라도 있으면 아무것도 지우지 않는다")
	@PostMapping("/roles/bulk-delete")
	public ResponseEntity<Void> deleteAll(@CurrentUser AuthPrincipal actor,
		@Valid @RequestBody RoleIdsRequest request) {
		accessRoleApplication.deleteAll(actor, request.ids());
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "역할 지우기",
		description = "소프트 삭제. 이 역할을 가진 사용자에게서도 빠진다. 없으면 R001, 다룰 수 없는 역할 A005. moa: 최고 관리자 역할은 B001")
	@DeleteMapping("/roles/{id}")
	public ResponseEntity<Void> delete(@CurrentUser AuthPrincipal actor, @PathVariable Long id) {
		accessRoleApplication.delete(actor, id);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "역할의 구성원",
		description = "moa (FR-006): 아이디 순 쪽. keyword가 없으면 구성원만, 있으면 아이디·이름으로 관리자 계정과 구성원 여부. "
			+ "지운 사용자·시스템 계정은 빠진다. 없는 역할 R001")
	@GetMapping("/roles/{id}/members")
	public ResponseEntity<PageResponse<RoleMemberResponse>> getMembers(@PathVariable Long id,
		@RequestParam(required = false) @Size(max = 50) String keyword,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return ResponseEntity.ok(PageResponse.from(
			accessRoleApplication.getMembers(id, keyword, PageRequest.of(page, size)), RoleMemberResponse::from));
	}

	@Operation(summary = "구성원 더하기",
		description = "moa (FR-006): 한 트랜잭션. 이미 구성원이면 그대로. 없는 역할·사용자 R001, 다룰 수 없는 역할 A005, "
			+ "회원이나 (다른 역할에) 최고 관리자가 섞이면 B001 (아무도 더하지 않는다). 최고 관리자 역할에 더하면 그 계정의 다른 역할은 빠진다")
	@PostMapping("/roles/{id}/members")
	public ResponseEntity<Void> addMembers(@CurrentUser AuthPrincipal actor, @PathVariable Long id,
		@Valid @RequestBody RoleMembersRequest request) {
		accessRoleApplication.addMembers(actor, id, request.userIds());
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "구성원 빼기",
		description = "moa (FR-006): 한 트랜잭션. 구성원이 아니면 그대로. 없는 역할·사용자 R001, 다룰 수 없는 역할 A005, "
			+ "회원이 섞이거나 최고 관리자가 0명이 되면 B001 (아무도 빼지 않는다)")
	@PostMapping("/roles/{id}/members/remove")
	public ResponseEntity<Void> removeMembers(@CurrentUser AuthPrincipal actor, @PathVariable Long id,
		@Valid @RequestBody RoleMembersRequest request) {
		accessRoleApplication.removeMembers(actor, id, request.userIds());
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "사용자의 역할", description = "없는 사용자 R001")
	@GetMapping("/users/{userId}/roles")
	public ResponseEntity<List<RoleResponse>> getRolesOfUser(@CurrentUser AuthPrincipal actor,
		@PathVariable Long userId) {
		return ResponseEntity
			.ok(accessRoleApplication.getRolesOfUser(actor, userId).stream().map(RoleResponse::from).toList());
	}

	@Operation(summary = "사용자의 역할 바꾸기",
		description = "보낸 역할로 통째로 바꾼다. 다음 요청부터 바로 반영된다. 없는 사용자·역할 R001, 주거나 빼는 역할을 다룰 수 없으면 A005. "
			+ "moa: 대상이 회원이면 B001, 최고 관리자가 0명이 되면 B001. 최고 관리자 역할을 고르면 그 역할만 남는다. 사용자 관리 드로어도 쓴다")
	@PutMapping("/users/{userId}/roles")
	public ResponseEntity<Void> replaceRolesOfUser(@CurrentUser AuthPrincipal actor, @PathVariable Long userId,
		@Valid @RequestBody UserRoleReplaceRequest request) {
		accessRoleApplication.replaceRolesOfUser(actor, userId, request.roleIds());
		return ResponseEntity.noContent().build();
	}
}
