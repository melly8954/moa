package com.moa.api.controller.v1.admin.role.response;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.role.AccessRoleDto;
import com.moa.api.dto.role.AccessRoleView;
import com.moa.api.dto.role.GrantBlock;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "역할")
public record RoleResponse(
	@Schema(description = "역할 ID") Long id,
	@Schema(description = "역할 코드", example = "MANAGER") String code,
	@Schema(description = "이름", example = "운영 매니저") String name,
	@Schema(description = "설명") @Nullable String description,
	@Schema(description = "묶은 권한 키 (이름순). 최고 관리자 역할은 비고 모든 권한을 갖는다",
		example = "[\"CODE_MANAGE\"]") List<String> permissions,
	@Schema(description = "이 역할을 가진 사용자 수", example = "3") long userCount,
	@Schema(description = "moa: 코드에 고정된 최고 관리자 역할인가. 바꾸거나 지울 수 없다 (specs/003-admin-roles FR-010)") boolean fixed,
	@Schema(description = "moa: 묶은 권한의 이름 (사용자 관리 드로어에 작게 보인다)", example = "[\"코드 관리\"]") List<String> permissionNames,
	@Schema(description = "moa: 요청한 사람이 이 역할을 다루고 주고 뺄 수 있나") boolean grantable,
	@Schema(description = "moa: 다룰 수 없는 이유. 다룰 수 있으면 null") @Nullable GrantBlock blockReason) {

	public static RoleResponse from(AccessRoleView view) {
		AccessRoleDto role = view.role();
		return new RoleResponse(role.id(), role.code(), role.name(), role.description(), role.permissions(),
			role.userCount(), role.fixed(), view.permissionNames(), view.grantable(), view.blockReason());
	}
}
