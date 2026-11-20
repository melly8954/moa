package com.moa.api.controller.v1.admin.screen;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.menu.MenuApplication;
import com.moa.api.controller.v1.admin.screen.response.ScreenResponse;
import com.moa.api.permission.MenuPermissions;
import com.moa.common.security.permission.RequirePermission;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 연결 화면 목록 (moa, specs/001-admin-menus FR-008). MENU_MANAGE 권한. 연결 화면은 코드에 등록하므로 조회만 한다.
 */
@Tag(name = "관리자 연결 화면 API")
@RequirePermission(MenuPermissions.MANAGE)
@RestController
@RequestMapping("/api/v1/admin/screens")
@RequiredArgsConstructor
public class ApiAdminScreenController {

	private final MenuApplication menuApplication;

	@Operation(summary = "연결 화면 목록", description = "메뉴에 걸 수 있는 화면. 관리 연결 화면 먼저, 그 안에서는 등록 순서")
	@GetMapping
	public ResponseEntity<List<ScreenResponse>> getScreens() {
		return ResponseEntity.ok(menuApplication.getScreens().stream().map(ScreenResponse::from).toList());
	}
}
