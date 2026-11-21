package com.moa.api.controller.v1.menu;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.menu.MenuApplication;
import com.moa.api.controller.v1.menu.response.MyMenuResponse;
import com.moa.api.dto.menu.MyMenuDto;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;

/**
 * 내 메뉴 (선택 헬퍼 menu). 화면이 사이드바·상단 메뉴를 그릴 때 쓴다.
 */
@Tag(name = "메뉴 API")
@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
public class ApiMenuController {

	private final MenuApplication menuApplication;

	@Operation(summary = "내 메뉴",
		description = "사용 중인 관리 메뉴 트리 (2단계, moa: 대상 ADMIN만). moa: 관리자 계정은 권한과 상관없이 모두 받고 "
			+ "메뉴마다 accessible(연결 화면의 권한이 있는가)을 받는다. 회원은 빈 목록이다. 하위 메뉴가 없는 묶음 메뉴는 빠진다 "
			+ "(specs/001-admin-menus FR-002, 2026-11-22)")
	@GetMapping("/me")
	public ResponseEntity<List<MyMenuResponse>> me(@CurrentUser AuthPrincipal user) {
		return ResponseEntity.ok(menuApplication.getMyMenus(user).stream().map(MyMenuResponse::from).toList());
	}

	@Operation(summary = "사용자 메뉴",
		description = "moa: 사용자 셸 메뉴 (대상 USER, specs/001-admin-menus FR-007). 로그인하지 않아도 부른다. "
			+ "공개 범위가 PUBLIC이면 모두가, MEMBER면 로그인한 사용자만 본다")
	@PermitAll
	@SecurityRequirements
	@GetMapping("/user")
	public ResponseEntity<List<MyMenuResponse>> user(@CurrentUser AuthPrincipal user) {
		return ResponseEntity.ok(
			menuApplication.getUserMenus(user).stream().map(MyMenuDto::open).map(MyMenuResponse::from).toList());
	}
}
