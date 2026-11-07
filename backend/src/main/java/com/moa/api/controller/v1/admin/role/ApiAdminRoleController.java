package com.moa.api.controller.v1.admin.role;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.role.AccessRoleApplication;
import com.moa.api.controller.v1.admin.role.request.RoleCreateRequest;
import com.moa.api.controller.v1.admin.role.request.RoleModifyRequest;
import com.moa.api.controller.v1.admin.role.request.UserRoleReplaceRequest;
import com.moa.api.controller.v1.admin.role.response.PermissionResponse;
import com.moa.api.controller.v1.admin.role.response.RoleResponse;
import com.moa.api.permission.RolePermissions;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.CurrentUser;
import com.moa.common.security.permission.RequirePermission;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 역할·권한 관리 (선택 헬퍼 rbac). ROLE_MANAGE 권한. 화면 경로는 /admin/roles.
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

	@Operation(summary = "역할 목록", description = "만든 순서대로")
	@GetMapping("/roles")
	public ResponseEntity<List<RoleResponse>> getRoles() {
		return ResponseEntity.ok(accessRoleApplication.getRoles().stream().map(RoleResponse::from).toList());
	}

	@Operation(summary = "역할 하나", description = "없으면 R001")
	@GetMapping("/roles/{id}")
	public ResponseEntity<RoleResponse> getRole(@PathVariable Long id) {
		return ResponseEntity.ok(RoleResponse.from(accessRoleApplication.getRole(id)));
	}

	@Operation(summary = "역할 만들기", description = "코드 형식·등록되지 않은 권한 V001, 내가 가지지 않은 권한 A005, 코드 중복 R002")
	@PostMapping("/roles")
	public ResponseEntity<RoleResponse> create(@CurrentUser AuthPrincipal actor,
		@Valid @RequestBody RoleCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(RoleResponse.from(accessRoleApplication.create(actor, request.toCommand())));
	}

	@Operation(summary = "역할 바꾸기",
		description = "이름·설명·권한을 바꾼다 (코드는 그대로). 없으면 R001, 등록되지 않은 권한 V001, 다룰 수 없는 역할·권한 A005")
	@PutMapping("/roles/{id}")
	public ResponseEntity<RoleResponse> modify(@CurrentUser AuthPrincipal actor, @PathVariable Long id,
		@Valid @RequestBody RoleModifyRequest request) {
		return ResponseEntity.ok(RoleResponse.from(accessRoleApplication.modify(actor, request.toCommand(id))));
	}

	@Operation(summary = "역할 지우기", description = "소프트 삭제. 이 역할을 가진 사용자에게서도 빠진다. 없으면 R001, 다룰 수 없는 역할 A005")
	@DeleteMapping("/roles/{id}")
	public ResponseEntity<Void> delete(@CurrentUser AuthPrincipal actor, @PathVariable Long id) {
		accessRoleApplication.delete(actor, id);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "사용자의 역할", description = "없는 사용자 R001")
	@GetMapping("/users/{userId}/roles")
	public ResponseEntity<List<RoleResponse>> getRolesOfUser(@PathVariable Long userId) {
		return ResponseEntity
			.ok(accessRoleApplication.getRolesOfUser(userId).stream().map(RoleResponse::from).toList());
	}

	@Operation(summary = "사용자의 역할 바꾸기",
		description = "보낸 역할로 통째로 바꾼다. 다음 요청부터 바로 반영된다. 없는 사용자·역할 R001, 주거나 빼는 역할을 다룰 수 없으면 A005")
	@PutMapping("/users/{userId}/roles")
	public ResponseEntity<Void> replaceRolesOfUser(@CurrentUser AuthPrincipal actor, @PathVariable Long userId,
		@Valid @RequestBody UserRoleReplaceRequest request) {
		accessRoleApplication.replaceRolesOfUser(actor, userId, request.roleIds());
		return ResponseEntity.noContent().build();
	}
}
