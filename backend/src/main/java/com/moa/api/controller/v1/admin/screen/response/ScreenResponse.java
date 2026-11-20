package com.moa.api.controller.v1.admin.screen.response;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.menu.MenuAudience;
import com.moa.api.dto.menu.ScreenDto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "연결 화면 (메뉴에 걸 수 있는 화면)")
public record ScreenResponse(
	@Schema(description = "연결 화면 키", example = "ADMIN_CODES") String key,
	@Schema(description = "이름", example = "코드 관리") String name,
	@Schema(description = "화면 경로", example = "/admin/codes") String path,
	@Schema(description = "대상 (ADMIN 관리 셸, USER 사용자 셸)", example = "ADMIN") MenuAudience audience,
	@Schema(description = "관리 연결 화면의 권한. 사용자 연결 화면은 없다") @Nullable ScreenPermission permission,
	@Schema(description = "로그인이 필요한가. 관리 연결 화면은 언제나 true") boolean signInRequired) {

	public static ScreenResponse from(ScreenDto screen) {
		return new ScreenResponse(screen.key(), screen.name(), screen.path(), screen.audience(),
			screen.permission() == null ? null : new ScreenPermission(screen.permission(), screen.permissionName()),
			screen.signInRequired());
	}

	@Schema(description = "권한")
	public record ScreenPermission(
		@Schema(description = "권한 키", example = "CODE_MANAGE") String key,
		@Schema(description = "권한 이름", example = "공통코드 관리") String name) {
	}
}
