package com.moa.api.controller.v1.admin.program.response;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.menu.MenuAudience;
import com.moa.api.dto.menu.ProgramDto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로그램 (메뉴에 걸 수 있는 화면)")
public record ProgramResponse(
	@Schema(description = "프로그램 키", example = "ADMIN_CODES") String key,
	@Schema(description = "이름", example = "코드 관리") String name,
	@Schema(description = "화면 경로", example = "/admin/codes") String path,
	@Schema(description = "대상 (ADMIN 관리 셸, USER 사용자 셸)", example = "ADMIN") MenuAudience audience,
	@Schema(description = "관리 프로그램의 권한. 사용자 프로그램은 없다") @Nullable ProgramPermission permission,
	@Schema(description = "로그인이 필요한가. 관리 프로그램은 언제나 true") boolean signInRequired) {

	public static ProgramResponse from(ProgramDto program) {
		return new ProgramResponse(program.key(), program.name(), program.path(), program.audience(),
			program.permission() == null ? null : new ProgramPermission(program.permission(), program.permissionName()),
			program.signInRequired());
	}

	@Schema(description = "권한")
	public record ProgramPermission(
		@Schema(description = "권한 키", example = "CODE_MANAGE") String key,
		@Schema(description = "권한 이름", example = "공통코드 관리") String name) {
	}
}
