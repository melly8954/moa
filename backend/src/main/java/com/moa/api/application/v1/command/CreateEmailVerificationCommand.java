package com.moa.api.application.v1.command;

/**
 * 가입 인증 메일 요청.
 *
 * @param email 가입할 이메일
 * @param password 비밀번호 원문. 해시로 바꿔 저장한다
 * @param signUpToken 카카오가 이메일을 주지 않아 이어 온 가입이면 그 가입 진행 토큰, 아니면 null
 */
public record CreateEmailVerificationCommand(String email, String password, String signUpToken) {
}
