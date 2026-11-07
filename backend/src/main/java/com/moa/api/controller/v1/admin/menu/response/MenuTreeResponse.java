package com.moa.api.controller.v1.admin.menu.response;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.menu.MenuDto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "메뉴 (관리)")
public record MenuTreeResponse(
	@Schema(description = "메뉴 ID") Long id,
	@Schema(description = "부모 메뉴 ID. 1단계면 없다") @Nullable Long parentId,
	@Schema(description = "이름", example = "코드 관리") String name,
	@Schema(description = "화면 경로. 그룹이면 없다", example = "/admin/codes") @Nullable String path,
	@Schema(description = "아이콘 이름", example = "list") @Nullable String icon,
	@Schema(description = "정렬 순서", example = "10") int sortOrder,
	@Schema(description = "사용 여부") boolean enabled,
	@Schema(description = "볼 때 필요한 권한 키. 없으면 로그인한 사용자 모두", example = "CODE_MANAGE") @Nullable String permission,
	@Schema(description = "하위 메뉴") List<MenuTreeResponse> children) {

	public static MenuTreeResponse from(MenuDto menu) {
		return new MenuTreeResponse(menu.id(), menu.parentId(), menu.name(), menu.path(), menu.icon(),
			menu.sortOrder(), menu.enabled(), menu.permission(),
			menu.children().stream().map(MenuTreeResponse::from).toList());
	}
}
