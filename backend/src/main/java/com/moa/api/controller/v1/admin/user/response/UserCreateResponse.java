package com.moa.api.controller.v1.admin.user.response;

import com.moa.api.dto.user.CreatedUserDto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "만든 사용자와 임시 비밀번호")
public record UserCreateResponse(
	@Schema(description = "만든 사용자") UserAdminResponse user,
	@Schema(description = "임시 비밀번호. 이 응답에서만 보인다. 사용자는 다음 로그인 때 바꿔야 한다",
		example = "aB3$kP9mQ2xZ") String temporaryPassword) {

	public static UserCreateResponse from(CreatedUserDto created) {
		return new UserCreateResponse(UserAdminResponse.from(created.user()), created.temporaryPassword());
	}
}
