package com.moa.common.security;

import java.util.Set;

/**
 * 인증 주체. 인증 인프라는 사용자 ID와 역할만 안다. 사용자 계정 정보는 사용자 도메인이 가진다.
 * 컨트롤러에서는 {@code @AuthenticationPrincipal AuthPrincipal principal}로 받는다.
 */
public record AuthPrincipal(Long userId, Set<String> roles) {

	public AuthPrincipal {
		roles = roles == null ? Set.of() : Set.copyOf(roles);
	}
}
