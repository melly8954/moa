package com.moa.api.controller.v1.admin.code;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.code.CodeApplication;
import com.moa.api.controller.v1.admin.code.request.CodeCreateRequest;
import com.moa.api.controller.v1.admin.code.request.CodeEnabledRequest;
import com.moa.api.controller.v1.admin.code.request.CodeGroupCreateRequest;
import com.moa.api.controller.v1.admin.code.request.CodeGroupModifyRequest;
import com.moa.api.controller.v1.admin.code.request.CodeModifyRequest;
import com.moa.api.controller.v1.admin.code.request.CodeOrderRequest;
import com.moa.api.controller.v1.admin.code.response.CodeGroupListResponse;
import com.moa.api.controller.v1.admin.code.response.CodeGroupTreeResponse;
import com.moa.api.controller.v1.code.response.CodeListResponse;
import com.moa.api.permission.CodePermissions;
import com.moa.common.security.permission.RequirePermission;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;

/**
 * 공통코드 관리 (선택 헬퍼 code). 관리자만. 코드를 바꾸면 그 그룹의 화면용 캐시를 지운다.
 * moa: 그룹과 항목은 지우지 않는다. 지우는 API가 없고 사용 여부로 끈다 (specs/004-admin-codes FR-008)
 */
@Tag(name = "관리자 공통코드 API")
@RequirePermission(CodePermissions.MANAGE)
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class ApiAdminCodeController {

	private final CodeApplication codeApplication;

	@Operation(summary = "코드 그룹 목록")
	@GetMapping("/code-groups")
	public ResponseEntity<List<CodeGroupListResponse>> getGroups() {
		return ResponseEntity.ok(codeApplication.getGroups().stream().map(CodeGroupListResponse::from).toList());
	}

	@Operation(summary = "코드 그룹 만들기",
		description = "moa: 사용 중으로 만든다. 그룹 코드가 겹치면 R002 (specs/004-admin-codes FR-004)")
	@PostMapping("/code-groups")
	public ResponseEntity<CodeGroupListResponse> createGroup(@Valid @RequestBody CodeGroupCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(CodeGroupListResponse.from(codeApplication.createGroup(request.toCommand())));
	}

	@Operation(summary = "moa: 코드 그룹 바꾸기", description = "이름, 설명, 사용 여부. 그룹 코드는 바꾸지 않는다")
	@PutMapping("/code-groups/{groupCode}")
	public ResponseEntity<CodeGroupListResponse> modifyGroup(@PathVariable String groupCode,
		@Valid @RequestBody CodeGroupModifyRequest request) {
		return ResponseEntity.ok(CodeGroupListResponse.from(codeApplication.modifyGroup(request.toCommand(groupCode))));
	}

	@Operation(summary = "moa: 공통코드 트리", description = "그룹마다(만든 순서) 항목(사용 안 함 포함, 정렬 순서대로)")
	@GetMapping("/code-groups/tree")
	public ResponseEntity<List<CodeGroupTreeResponse>> getTree() {
		return ResponseEntity.ok(codeApplication.getTree().stream().map(CodeGroupTreeResponse::from).toList());
	}

	@Operation(summary = "그룹의 코드 목록 (사용 안 함 포함)")
	@GetMapping("/code-groups/{groupCode}/codes")
	public ResponseEntity<List<CodeListResponse>> getCodes(@PathVariable String groupCode) {
		return ResponseEntity.ok(codeApplication.getAllCodes(groupCode).stream().map(CodeListResponse::from)
			.toList());
	}

	@Operation(summary = "코드 만들기",
		description = "그룹이 없으면 R001, 같은 코드가 있으면 R002. moa: 코드를 비우면 자동 순번, 순서를 비우면 맨 뒤")
	@PostMapping("/code-groups/{groupCode}/codes")
	public ResponseEntity<CodeListResponse> createCode(@PathVariable String groupCode,
		@Valid @RequestBody CodeCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(CodeListResponse.from(codeApplication.createCode(request.toCommand(groupCode))));
	}

	@Operation(summary = "코드 바꾸기", description = "이름, 정렬 순서(moa: 비우면 그대로), 사용 여부")
	@PutMapping("/code-groups/{groupCode}/codes/{code}")
	public ResponseEntity<CodeListResponse> modifyCode(@PathVariable String groupCode, @PathVariable String code,
		@Valid @RequestBody CodeModifyRequest request) {
		return ResponseEntity.ok(CodeListResponse.from(codeApplication.modifyCode(request.toCommand(groupCode, code))));
	}

	@Operation(summary = "moa: 그룹 안 순서 한꺼번에", description = "다른 그룹의 코드가 섞이면 R001, 아무것도 바꾸지 않는다")
	@PutMapping("/code-groups/{groupCode}/codes/order")
	public ResponseEntity<Void> reorder(@PathVariable String groupCode,
		@RequestBody @NotEmpty List<@Valid CodeOrderRequest> request) {
		codeApplication.reorder(groupCode, request.stream().map(CodeOrderRequest::toDto).toList());
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "moa: 그룹 안 여러 항목 사용 여부", description = "하나라도 없으면 R001, 아무것도 바꾸지 않는다")
	@PutMapping("/code-groups/{groupCode}/codes/enabled")
	public ResponseEntity<Void> setEnabled(@PathVariable String groupCode,
		@Valid @RequestBody CodeEnabledRequest request) {
		codeApplication.setEnabled(groupCode, request.codes(), request.enabled());
		return ResponseEntity.noContent().build();
	}

}
