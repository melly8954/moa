package com.moa.api.controller.v1.admin.menu.response;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.menu.MenuAudience;
import com.moa.api.dto.menu.MenuDto;
import com.moa.api.dto.menu.MenuVisibility;
import com.moa.api.dto.menu.ScreenDto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "메뉴 (관리)")
public record MenuTreeResponse(
	@Schema(description = "메뉴 ID") Long id,
	@Schema(description = "부모 메뉴 ID. 1단계면 없다") @Nullable Long parentId,
	@Schema(description = "이름", example = "코드 관리") String name,
	@Schema(description = "아이콘 이름", example = "list") @Nullable String icon,
	@Schema(description = "정렬 순서", example = "10") int sortOrder,
	@Schema(description = "사용 여부") boolean enabled,
	@Schema(description = "moa: 메뉴 대상 (ADMIN 관리 셸, USER 사용자 셸)", example = "ADMIN") MenuAudience audience,
	@Schema(description = "moa: 가리키는 연결 화면 키. 묶음 메뉴면 없다", example = "ADMIN_CODES") @Nullable String screenKey,
	@Schema(description = "moa: 가리키는 연결 화면. 묶음 메뉴이거나 없는 연결 화면이면 없다") @Nullable MenuScreen screen,
	@Schema(description = "moa: 연결 화면 키가 있는데 등록된 연결 화면이 없다 (코드에서 빠졌다). 사이드바에 보이지 않는다") boolean missingScreen,
	@Schema(description = "moa: 사용자 화면 메뉴의 공개 범위. 관리 메뉴·묶음 메뉴는 없다",
		example = "PUBLIC") @Nullable MenuVisibility visibility,
	@Schema(description = "하위 메뉴") List<MenuTreeResponse> children) {

	public static MenuTreeResponse from(MenuDto menu) {
		return new MenuTreeResponse(menu.id(), menu.parentId(), menu.name(), menu.icon(), menu.sortOrder(),
			menu.enabled(), menu.audience(), menu.screenKey(),
			menu.screen() == null ? null : MenuScreen.from(menu.screen()), menu.missingScreen(),
			menu.visibility(), menu.children().stream().map(MenuTreeResponse::from).toList());
	}

	@Schema(description = "메뉴가 가리키는 연결 화면")
	public record MenuScreen(
		@Schema(description = "이름", example = "코드 관리") String name,
		@Schema(description = "화면 경로", example = "/admin/codes") String path,
		@Schema(description = "관리 연결 화면의 권한 키. 사용자 연결 화면은 없다", example = "CODE_MANAGE") @Nullable String permission,
		@Schema(description = "관리 연결 화면의 권한 이름", example = "공통코드 관리") @Nullable String permissionName,
		@Schema(description = "로그인이 필요한가. 관리 연결 화면은 언제나 true") boolean signInRequired) {

		static MenuScreen from(ScreenDto screen) {
			return new MenuScreen(screen.name(), screen.path(), screen.permission(), screen.permissionName(),
				screen.signInRequired());
		}
	}
}
