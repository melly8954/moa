package com.moa.api.controller.v1.admin.user.response;

import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.user.UserAdminDto;
import com.moa.common.security.Role;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 (관리)")
public record UserAdminResponse(
	@Schema(description = "사용자 ID") Long id,
	@Schema(description = "로그인 아이디", example = "hong") String loginId,
	@Schema(description = "이름", example = "홍길동") String name,
	@Schema(description = "시스템 역할", example = "USER") Role role,
	@Schema(description = "잠김 여부") boolean locked,
	@Schema(description = "잠근 시각. 잠기지 않았으면 없다") @Nullable LocalDateTime lockedAt,
	@Schema(description = "임시 비밀번호를 받아 아직 바꾸지 않았다") boolean passwordChangeRequired,
	@Schema(description = "비밀번호로 로그인하는 계정인지. false(소셜)면 비밀번호 초기화를 할 수 없다") boolean hasPassword,
	@Schema(description = "만든 시각") LocalDateTime createdAt) {

	public static UserAdminResponse from(UserAdminDto user) {
		return new UserAdminResponse(user.id(), user.loginId(), user.name(), user.role(), user.locked(),
			user.lockedAt(), user.passwordChangeRequired(), user.hasPassword(), user.createdAt());
	}
}
