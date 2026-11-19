package com.moa.api.application.v1.auth.command;

import java.util.List;

import com.moa.api.dto.user.TermsAgreementDto;

/**
 * 소셜 가입의 닉네임과 동의한 약관 (specs/010-social-login contracts "sign-up").
 */
public record SocialSignUpCommand(String nickname, List<TermsAgreementDto> agreements) {
}
