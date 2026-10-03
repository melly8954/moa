package com.moa.api.controller.v1.request;

import com.moa.api.dto.MemberPolicy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "문자 인증 번호 요청")
public record PhoneVerificationCreateRequest(
	@Schema(description = "휴대폰 번호. 숫자만",
		example = "01012345678") @NotBlank @Pattern(regexp = MemberPolicy.PHONE_NUMBER_PATTERN) String phoneNumber) {
}
