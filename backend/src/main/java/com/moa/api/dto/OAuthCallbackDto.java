package com.moa.api.dto;

import com.moa.common.security.token.IssuedTokens;

/**
 * 소셜 인증 콜백 처리 결과. 컨트롤러는 쿠키를 쓰고 redirectUrl로 보낸다.
 *
 * @param redirectUrl 프론트 주소
 *     <ul>
 *       <li>로그인(같은 이메일 계정에 합침 포함): {@code <웹>/}</li>
 *       <li>새 가입, 이메일 있음: {@code <웹>/signup/phone}</li>
 *       <li>새 가입, 카카오가 이메일을 주지 않음: {@code <웹>/signup/email}</li>
 *       <li>실패: {@code <웹>/signup?error=<오류 코드>} (가입에서 시작) 또는 {@code <웹>/login?error=<오류 코드>}</li>
 *     </ul>
 * @param signUpToken 가입 진행 토큰 원문. 새 가입일 때만 있다
 * @param tokens 발급한 토큰. 로그인일 때만 있다 (리프레시 토큰만 쿠키로 쓴다)
 */
public record OAuthCallbackDto(String redirectUrl, String signUpToken, IssuedTokens tokens) {
}
