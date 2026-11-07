package com.moa.api.application.v1.auth;

import org.springframework.stereotype.Component;

import com.moa.api.application.v1.auth.command.LoginCommand;
import com.moa.api.dto.user.UserDto;
import com.moa.api.service.user.UserService;
import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.token.AuthTokenService;
import com.moa.common.security.token.IssuedTokens;

import lombok.RequiredArgsConstructor;

/**
 * 로그인과 토큰 API의 진입점. 다른 로그인 방식(소셜 등)을 더할 때도 인증에 성공한 뒤 issue를 부른다.
 * 트랜잭션은 공통 인프라 AuthTokenService가 가진다. 여기에 @Transactional을 붙이지 않는다
 * (리프레시 토큰 재사용을 감지하면 그 사용자의 토큰 폐기를 커밋한 뒤 A004를 던져야 한다. 바깥 트랜잭션이 있으면 폐기도 롤백된다).
 */
@Component
@RequiredArgsConstructor
public class AuthTokenApplication {

	private final AuthTokenService authTokenService;
	private final UserService userService;

	/**
	 * 아이디·비밀번호로 로그인한다.
	 *
	 * @throws com.moa.common.exception.ServiceException 아이디·비밀번호가 틀리면 A006
	 */
	public IssuedTokens login(LoginCommand command) {
		UserDto user = userService.authenticate(command.loginId(), command.password());
		return authTokenService.issue(user.toPrincipal());
	}

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
