package com.moa.api.controller.v1.admin.menu;

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

import com.moa.api.application.v1.menu.MenuApplication;
import com.moa.api.controller.v1.admin.menu.request.MenuCreateRequest;
import com.moa.api.controller.v1.admin.menu.request.MenuModifyRequest;
import com.moa.api.controller.v1.admin.menu.request.MenuOrderRequest;
import com.moa.api.controller.v1.admin.menu.response.MenuTreeResponse;
import com.moa.api.permission.MenuPermissions;
import com.moa.common.security.permission.RequirePermission;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;

/**
 * 메뉴 관리 (선택 헬퍼 menu). MENU_MANAGE 권한. 화면 경로는 /admin/menus.
 * 메뉴를 숨겨도 API는 막히지 않는다. API 보호는 컨트롤러의 @RequirePermission이 한다.
 */
@Tag(name = "관리자 메뉴 API")
@RequirePermission(MenuPermissions.MANAGE)
@RestController
@RequestMapping("/api/v1/admin/menus")
@RequiredArgsConstructor
public class ApiAdminMenuController {

	private final MenuApplication menuApplication;

	@Operation(summary = "메뉴 트리 (사용 안 함 포함)")
	@GetMapping
	public ResponseEntity<List<MenuTreeResponse>> getMenus() {
		return ResponseEntity.ok(menuApplication.getAllMenus().stream().map(MenuTreeResponse::from).toList());
	}

	@Operation(summary = "메뉴 만들기",
		description = "경로 형식·등록되지 않은 권한 V001, 경로 중복 R002, 없는 부모 R001, 3단계가 되는 부모 B001")
	@PostMapping
	public ResponseEntity<MenuTreeResponse> create(@Valid @RequestBody MenuCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(MenuTreeResponse.from(menuApplication.create(request.toCommand())));
	}

	@Operation(summary = "순서 한 번에 바꾸기",
		description = "보낸 메뉴의 부모와 순서를 한 트랜잭션으로 바꾼다. 하나라도 틀리면 아무것도 바꾸지 않는다")
	@PutMapping("/order")
	public ResponseEntity<Void> reorder(@RequestBody @NotEmpty List<@Valid MenuOrderRequest> request) {
		menuApplication.reorder(request.stream().map(MenuOrderRequest::toDto).toList());
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "메뉴 바꾸기", description = "없는 메뉴 R001, 그 밖의 오류는 만들기와 같다. 자기 자신을 부모로 B001")
	@PutMapping("/{id}")
	public ResponseEntity<MenuTreeResponse> modify(@PathVariable Long id,
		@Valid @RequestBody MenuModifyRequest request) {
		return ResponseEntity.ok(MenuTreeResponse.from(menuApplication.modify(request.toCommand(id))));
	}

	@Operation(summary = "메뉴 지우기", description = "소프트 삭제. 하위 메뉴가 있으면 B001")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		menuApplication.delete(id);
		return ResponseEntity.noContent().build();
	}
}
