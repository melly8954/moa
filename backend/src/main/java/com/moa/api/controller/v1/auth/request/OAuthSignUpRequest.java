package com.moa.api.controller.v1.auth.request;

import java.util.List;

import com.moa.api.application.v1.auth.command.SocialSignUpCommand;
import com.moa.api.dto.user.TermsAgreementDto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 소셜 가입 요청 (specs/010-social-login contracts "sign-up"). 닉네임 규칙(NicknamePolicy)과 약관 버전은 서비스가 확인한다.
 */
@Schema(description = "소셜 가입 요청")
public record OAuthSignUpRequest(
	@Schema(description = "닉네임. 2~12자 한글·영문·숫자·_ (잠정). 다른 회원과 겹쳐도 된다",
		example = "모아_01") @NotNull @Size(max = 50) String nickname,
	@Schema(description = "동의한 약관. SERVICE·PRIVACY 둘 다 현재 버전이어야 한다") @NotNull @Size(
		max = 10) List<@Valid Agreement> agreements) {

	public SocialSignUpCommand toCommand() {
		return new SocialSignUpCommand(nickname,
			agreements.stream().map(agreement -> new TermsAgreementDto(agreement.type(), agreement.version()))
				.toList());
	}

	@Schema(description = "동의한 약관 하나")
	public record Agreement(
		@Schema(description = "종류 (SERVICE, PRIVACY)", example = "SERVICE") @NotBlank @Size(max = 20) String type,
		@Schema(description = "동의한 버전 (GET /api/v1/terms)",
			example = "draft") @NotBlank @Size(max = 20) String version) {
	}
}
