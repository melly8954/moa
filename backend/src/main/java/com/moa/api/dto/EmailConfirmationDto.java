package com.moa.api.dto;

import com.moa.common.security.token.IssuedTokens;

/**
 * 이메일 인증 결과.
 *
 * @param result 결과
 * @param signUpToken 가입 진행 토큰 원문. CONTINUE_SIGN_UP일 때만 있다
 * @param tokens 발급한 토큰. SIGNED_IN일 때만 있다
 */
public record EmailConfirmationDto(EmailConfirmationResult result, String signUpToken, IssuedTokens tokens) {
}
