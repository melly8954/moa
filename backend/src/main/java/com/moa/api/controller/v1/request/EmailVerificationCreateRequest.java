package com.moa.api.controller.v1.request;

import com.moa.api.dto.MemberPolicy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "가입 인증 메일 요청. 비밀번호 확인 입력은 화면에서만 검사한다")
public record EmailVerificationCreateRequest(
	@Schema(description = "가입할 이메일",
		example = "gamer@naver.com") @NotBlank @Email @Size(max = MemberPolicy.EMAIL_MAX_LENGTH) String email,
	@Schema(description = "비밀번호. 8~20자, 영문과 숫자를 각각 1자 이상") @NotBlank @Pattern(
		regexp = MemberPolicy.PASSWORD_PATTERN) String password) {
}
