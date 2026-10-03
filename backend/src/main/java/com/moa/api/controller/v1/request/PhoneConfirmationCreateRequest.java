package com.moa.api.controller.v1.request;

import java.time.LocalDate;

import com.moa.api.dto.MemberPolicy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

@Schema(description = "문자 인증 번호 확인과 생년월일")
public record PhoneConfirmationCreateRequest(
	@Schema(description = "인증 번호 6자리",
		example = "123456") @NotBlank @Pattern(regexp = MemberPolicy.VERIFICATION_CODE_PATTERN) String verificationCode,
	@Schema(description = "생년월일", example = "2000-01-31") @NotNull @Past LocalDate birthDate) {
}
