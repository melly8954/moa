package com.moa.support;

import java.util.Set;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.token.AuthPrincipalLoader;

/**
 * 사용자 도메인이 없는 골격에서 토큰 재발급을 시험하기 위한 AuthPrincipalLoader.
 * 사용자 도메인이 AuthPrincipalLoader를 구현하면 이 클래스를 지우고 AcceptanceTest의 @Import에서 뺀다.
 * 남겨 두면 로더 빈이 둘이 되어 재발급이 S001로 실패한다 (APPLY.md 적용 후 할 일 3).
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestAuthConfig {

	@Bean
	public AuthPrincipalLoader testAuthPrincipalLoader() {
		return userId -> new AuthPrincipal(userId, Set.of("USER"));
	}
}
