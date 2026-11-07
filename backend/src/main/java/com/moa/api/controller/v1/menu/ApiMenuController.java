package com.moa.api.controller.v1.menu;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moa.api.application.v1.menu.MenuApplication;
import com.moa.api.controller.v1.menu.response.MyMenuResponse;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
		description = "사용 중이고 내 권한으로 볼 수 있는 메뉴 트리 (2단계). 권한 키가 없는 메뉴는 모두가 본다. ADMIN은 모두 본다. "
			+ "보이는 하위 메뉴가 없는 그룹은 빠진다")
	@GetMapping("/me")
	public ResponseEntity<List<MyMenuResponse>> me(@CurrentUser AuthPrincipal user) {
		return ResponseEntity.ok(menuApplication.getMyMenus(user).stream().map(MyMenuResponse::from).toList());
	}
}
