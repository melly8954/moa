package com.moa.api.controller.v1.menu.response;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.menu.MyMenuDto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 메뉴")
public record MyMenuResponse(
	@Schema(description = "메뉴 ID") Long id,
	@Schema(description = "이름", example = "코드 관리") String name,
	@Schema(description = "화면 경로. 그룹이면 없다", example = "/admin/codes") @Nullable String path,
	@Schema(description = "아이콘 이름", example = "list") @Nullable String icon,
	@Schema(description = "moa: 들어갈 수 있는가(연결 화면의 권한이 있는가). 상위 메뉴는 하위 메뉴 중 하나라도 있으면 true") boolean accessible,
	@Schema(description = "하위 메뉴") List<MyMenuResponse> children) {

	public static MyMenuResponse from(MyMenuDto menu) {
		return new MyMenuResponse(menu.id(), menu.name(), menu.path(), menu.icon(), menu.accessible(),
			menu.children().stream().map(MyMenuResponse::from).toList());
	}
}
