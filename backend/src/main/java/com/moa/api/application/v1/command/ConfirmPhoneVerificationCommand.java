package com.moa.api.application.v1.command;

import java.time.LocalDate;

/**
 * 문자 인증 번호 확인과 생년월일 입력.
 *
 * @param signUpToken 가입 진행 토큰
 * @param verificationCode 인증 번호
 * @param birthDate 생년월일
 */
public record ConfirmPhoneVerificationCommand(String signUpToken, String verificationCode, LocalDate birthDate) {
}
