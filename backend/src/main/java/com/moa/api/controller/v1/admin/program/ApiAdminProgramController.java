package com.moa.api.controller.v1.admin.program;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.menu.MenuApplication;
import com.moa.api.controller.v1.admin.program.response.ProgramResponse;
import com.moa.api.permission.MenuPermissions;
import com.moa.common.security.permission.RequirePermission;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 프로그램 목록 (moa, specs/001-admin-menus FR-008). MENU_MANAGE 권한. 프로그램은 코드에 등록하므로 조회만 한다.
 */
@Tag(name = "관리자 프로그램 API")
@RequirePermission(MenuPermissions.MANAGE)
@RestController
@RequestMapping("/api/v1/admin/programs")
@RequiredArgsConstructor
public class ApiAdminProgramController {

	private final MenuApplication menuApplication;

	@Operation(summary = "프로그램 목록", description = "메뉴에 걸 수 있는 화면. 관리 프로그램 먼저, 그 안에서는 등록 순서")
	@GetMapping
	public ResponseEntity<List<ProgramResponse>> getPrograms() {
		return ResponseEntity.ok(menuApplication.getPrograms().stream().map(ProgramResponse::from).toList());
	}
}
