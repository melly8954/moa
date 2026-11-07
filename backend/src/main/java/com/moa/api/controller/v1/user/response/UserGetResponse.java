package com.moa.api.controller.v1.user.response;

import com.moa.api.dto.user.UserDto;
import com.moa.common.security.Role;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자")
public record UserGetResponse(
	@Schema(description = "사용자 ID", example = "3") Long id,
	@Schema(description = "로그인 아이디", example = "user") String loginId,
	@Schema(description = "이름", example = "사용자") String name,
	@Schema(description = "역할", example = "USER") Role role,
	@Schema(description = "임시 비밀번호를 받아 바꿔야 한다. true면 화면이 비밀번호 변경으로 보낸다") boolean passwordChangeRequired) {

	public static UserGetResponse from(UserDto user) {
		return new UserGetResponse(user.id(), user.loginId(), user.name(), user.role(), user.passwordChangeRequired());
	}
}
