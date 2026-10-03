package com.moa.api.controller.v1.request;

import com.moa.api.dto.MemberPolicy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "가입 완료. 이메일·비밀번호·제공자 계정·생년월일·휴대폰 번호는 가입 진행에서 가져온다")
public record MemberCreateRequest(
	@Schema(description = "닉네임. 2~12자, 한글·영문·숫자·밑줄",
		example = "moa_gamer") @NotBlank @Pattern(regexp = MemberPolicy.NICKNAME_PATTERN) String nickname,
	@Schema(description = "[필수] 이용약관 동의") @AssertTrue boolean termsAgreed,
	@Schema(description = "[필수] 개인정보 수집·이용 동의 (이메일, 생년월일, 휴대폰 번호)") @AssertTrue boolean privacyAgreed) {
}
