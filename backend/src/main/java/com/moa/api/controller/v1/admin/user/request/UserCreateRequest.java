package com.moa.api.controller.v1.admin.user.request;

import com.moa.common.security.Role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 만들기 요청. 비밀번호는 서버가 임시로 만든다")
public record UserCreateRequest(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "로그인 아이디. 영문·숫자·점·밑줄·하이픈 3~50자",
		example = "hong") @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{3,50}") String loginId,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "이름",
		example = "홍길동") @NotBlank @Size(max = 50) String name,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "시스템 역할. ADMIN은 ADMIN만 줄 수 있다",
		example = "USER") @NotNull Role role) {
}
