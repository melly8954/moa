package com.moa.api.application.v1;

import org.springframework.stereotype.Component;

import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.token.AuthTokenService;
import com.moa.common.security.token.IssuedTokens;

import lombok.RequiredArgsConstructor;

/**
 * 토큰 API의 진입점. 로그인 API를 만들 때도 인증에 성공한 뒤 issue를 부른다.
 */
@Component
@RequiredArgsConstructor
public class AuthTokenApplication {

	private final AuthTokenService authTokenService;

	public IssuedTokens issue(AuthPrincipal principal) {
		return authTokenService.issue(principal);
	}

	public IssuedTokens refresh(String refreshToken) {
		return authTokenService.refresh(refreshToken);
	}

	public void logout(String refreshToken) {
		authTokenService.revoke(refreshToken);
	}
}
