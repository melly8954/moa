package com.moa.api.controller.v1.admin.code;

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

import com.moa.api.application.v1.code.CodeApplication;
import com.moa.api.controller.v1.admin.code.request.CodeCreateRequest;
import com.moa.api.controller.v1.admin.code.request.CodeGroupCreateRequest;
import com.moa.api.controller.v1.admin.code.request.CodeModifyRequest;
import com.moa.api.controller.v1.admin.code.response.CodeGroupListResponse;
import com.moa.api.controller.v1.code.response.CodeListResponse;
import com.moa.api.permission.CodePermissions;
import com.moa.common.security.permission.RequirePermission;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 공통코드 관리 (선택 헬퍼 code). 관리자만. 코드를 바꾸면 그 그룹의 화면용 캐시를 지운다.
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

	@Operation(summary = "코드 그룹 만들기", description = "같은 그룹 코드가 있으면 R002")
	@PostMapping("/code-groups")
	public ResponseEntity<CodeGroupListResponse> createGroup(@Valid @RequestBody CodeGroupCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(CodeGroupListResponse.from(
			codeApplication.createGroup(request.groupCode(), request.name(), request.description())));
	}

	@Operation(summary = "그룹의 코드 목록 (사용 안 함 포함)")
	@GetMapping("/code-groups/{groupCode}/codes")
	public ResponseEntity<List<CodeListResponse>> getCodes(@PathVariable String groupCode) {
		return ResponseEntity.ok(codeApplication.getAllCodes(groupCode).stream().map(CodeListResponse::from)
			.toList());
	}

	@Operation(summary = "코드 만들기", description = "그룹이 없으면 R001, 같은 코드가 있으면 R002")
	@PostMapping("/code-groups/{groupCode}/codes")
	public ResponseEntity<CodeListResponse> createCode(@PathVariable String groupCode,
		@Valid @RequestBody CodeCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(CodeListResponse.from(codeApplication.createCode(request.toCommand(groupCode))));
	}

	@Operation(summary = "코드 바꾸기", description = "이름, 정렬 순서, 사용 여부")
	@PutMapping("/code-groups/{groupCode}/codes/{code}")
	public ResponseEntity<CodeListResponse> modifyCode(@PathVariable String groupCode, @PathVariable String code,
		@Valid @RequestBody CodeModifyRequest request) {
		return ResponseEntity.ok(CodeListResponse.from(codeApplication.modifyCode(request.toCommand(groupCode, code))));
	}

	@Operation(summary = "코드 지우기", description = "소프트 삭제. 같은 코드로 다시 만들 수 있다")
	@DeleteMapping("/code-groups/{groupCode}/codes/{code}")
	public ResponseEntity<Void> deleteCode(@PathVariable String groupCode, @PathVariable String code) {
		codeApplication.deleteCode(groupCode, code);
		return ResponseEntity.noContent().build();
	}
}
