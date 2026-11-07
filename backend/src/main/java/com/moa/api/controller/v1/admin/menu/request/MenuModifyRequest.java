package com.moa.api.controller.v1.admin.menu.request;

import com.moa.api.application.v1.menu.command.ModifyMenuCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "메뉴 바꾸기 요청")
public record MenuModifyRequest(
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "부모 메뉴 ID. 없으면 1단계") Long parentId,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "이름",
		example = "코드 관리") @NotBlank @Size(max = 50) String name,
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "화면 경로 (/로 시작). 그룹이면 비운다",
		example = "/admin/codes") @Size(max = 200) String path,
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "아이콘 이름",
		example = "list") @Size(max = 50) String icon,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "정렬 순서",
		example = "10") @NotNull @Min(0) Integer sortOrder,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
		description = "사용 여부. false면 내 메뉴에서 빠진다") @NotNull Boolean enabled,
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED,
		description = "볼 때 필요한 권한 키. 비우면 로그인한 사용자 모두가 본다. 등록되지 않은 키는 V001",
		example = "CODE_MANAGE") @Size(max = 100) String permission) {

	public ModifyMenuCommand toCommand(Long id) {
		return new ModifyMenuCommand(id, parentId, name, path, icon, sortOrder, enabled, permission);
	}
}
