package com.moa.api.application.v1.command;

/**
 * 문자 인증 번호 요청.
 *
 * @param signUpToken 가입 진행 토큰
 * @param phoneNumber 휴대폰 번호(숫자만)
 */
public record CreatePhoneVerificationCommand(String signUpToken, String phoneNumber) {
}
