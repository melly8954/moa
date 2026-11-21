package com.moa.api.controller.v1.admin.user.response;

import java.time.OffsetDateTime;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.role.GrantBlock;
import com.moa.api.dto.user.UserAdminDto;
import com.moa.api.dto.user.UserAdminRowDto;
import com.moa.common.response.ApiTimes;
import com.moa.common.security.Role;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 (관리)")
public record UserAdminResponse(
	@Schema(description = "사용자 ID") Long id,
	@Schema(description = "로그인 아이디", example = "hong") String loginId,
	@Schema(description = "이름", example = "홍길동") String name,
	@Schema(description = "계정 종류 (moa: USER 회원, ADMIN 관리자 계정)", example = "USER") Role role,
	@Schema(description = "잠김 여부") boolean locked,
	@Schema(description = "잠근 시각 (시간대 포함). 잠기지 않았으면 없다") @Nullable OffsetDateTime lockedAt,
	@Schema(description = "임시 비밀번호를 받아 아직 바꾸지 않았다") boolean passwordChangeRequired,
	@Schema(description = "비밀번호로 로그인하는 계정인지. false(소셜)면 비밀번호 초기화를 할 수 없다") boolean hasPassword,
	@Schema(description = "만든 시각 (시간대 포함)") OffsetDateTime createdAt,
	@Schema(description = "moa: 받은 역할 (specs/002-admin-users FR-006). 목록·하나 조회에서만 채우고 "
		+ "바꾸기 응답에서는 비어 있다. 회원은 비어 있다") List<AccessRoleRef> accessRoles,
	@Schema(description = "moa: 최고 관리자 역할을 가졌나. 목록·하나 조회에서만 채운다") boolean superAdmin,
	@Schema(description = "moa: 요청한 사람이 이 계정을 다룰 수 없는 이유 (research R10). 다룰 수 있으면 null. "
		+ "목록·하나 조회에서만 채운다") @Nullable GrantBlock protection) {

	public static UserAdminResponse from(UserAdminDto user) {
		return of(user, List.of(), false, null);
	}

	public static UserAdminResponse from(UserAdminRowDto row) {
		return of(row.user(),
			row.accessRoles().stream().map(role -> new AccessRoleRef(role.id(), role.name())).toList(),
			row.superAdmin(), row.protection());
	}

	private static UserAdminResponse of(UserAdminDto user, List<AccessRoleRef> accessRoles, boolean superAdmin,
		@Nullable GrantBlock protection) {
		return new UserAdminResponse(user.id(), user.loginId(), user.name(), user.role(), user.locked(),
			ApiTimes.of(user.lockedAt()), user.passwordChangeRequired(), user.hasPassword(),
			ApiTimes.of(user.createdAt()), accessRoles, superAdmin, protection);
	}

	@Schema(description = "역할")
	public record AccessRoleRef(
		@Schema(description = "역할 ID") Long id,
		@Schema(description = "이름", example = "보안 담당") String name) {
	}
}
