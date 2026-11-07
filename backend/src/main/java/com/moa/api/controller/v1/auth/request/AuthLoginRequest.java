package com.moa.api.controller.v1.auth.request;

import com.moa.api.application.v1.auth.command.LoginCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "로그인 요청")
public record AuthLoginRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "로그인 아이디",
		example = "user") @NotBlank @Size(max = 50) String loginId,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "비밀번호",
		example = "user1234!") @NotBlank @Size(max = 100) String password) {

	public LoginCommand toCommand() {
		return new LoginCommand(loginId, password);
	}
}
