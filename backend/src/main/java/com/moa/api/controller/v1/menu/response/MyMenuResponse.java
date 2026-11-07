package com.moa.api.controller.v1.menu.response;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.menu.MenuDto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 메뉴")
public record MyMenuResponse(
	@Schema(description = "메뉴 ID") Long id,
	@Schema(description = "이름", example = "코드 관리") String name,
	@Schema(description = "화면 경로. 그룹이면 없다", example = "/admin/codes") @Nullable String path,
	@Schema(description = "아이콘 이름", example = "list") @Nullable String icon,
	@Schema(description = "하위 메뉴") List<MyMenuResponse> children) {

	public static MyMenuResponse from(MenuDto menu) {
		return new MyMenuResponse(menu.id(), menu.name(), menu.path(), menu.icon(),
			menu.children().stream().map(MyMenuResponse::from).toList());
	}
}
