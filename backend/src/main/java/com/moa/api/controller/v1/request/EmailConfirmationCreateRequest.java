package com.moa.api.controller.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "이메일 인증 링크 확인. 화면이 링크의 token 값을 그대로 보낸다")
public record EmailConfirmationCreateRequest(
	@Schema(description = "인증 링크의 token") @NotBlank String token) {
}
