package com.moa.api.controller.v1.admin.menu.request;

import com.moa.api.application.v1.menu.command.CreateMenuCommand;
import com.moa.api.dto.menu.MenuAudience;
import com.moa.api.dto.menu.MenuVisibility;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "메뉴 만들기 요청")
public record MenuCreateRequest(
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "부모 메뉴 ID. 없으면 1단계") Long parentId,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "이름",
		example = "코드 관리") @NotBlank @Size(max = 50) String name,
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED,
		description = "moa: 가리키는 프로그램 키 (GET /api/v1/admin/programs). 비우면 묶음 메뉴(1단계만). 등록되지 않았거나 대상이 다르면 V001",
		example = "ADMIN_CODES") @Size(max = 50) String programKey,
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "아이콘 이름",
		example = "list") @Size(max = 50) String icon,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "정렬 순서. 작을수록 앞",
		example = "10") @NotNull @Min(0) Integer sortOrder,
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED,
		description = "moa: 메뉴 대상 (ADMIN 관리 셸, USER 사용자 셸). 비우면 ADMIN. 하위 메뉴는 보낸 값과 상관없이 부모를 따른다",
		example = "ADMIN") MenuAudience audience,
	@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED,
		description = "moa: 사용자 화면 메뉴의 공개 범위 (PUBLIC 모두, MEMBER 로그인한 사용자). "
			+ "사용자 화면 메뉴는 필수, 로그인이 필요한 프로그램은 MEMBER만(V001). 관리 메뉴·묶음 메뉴는 버린다",
		example = "PUBLIC") MenuVisibility visibility) {

	public CreateMenuCommand toCommand() {
		return new CreateMenuCommand(parentId, name, programKey, icon, sortOrder, audience, visibility);
	}
}
