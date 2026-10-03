package com.moa.api.application.v1.command;

/**
 * 이메일 인증 링크 확인.
 *
 * @param token 인증 링크의 토큰 원문
 */
public record ConfirmEmailVerificationCommand(String token) {
}
