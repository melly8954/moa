package com.moa.common.security.access;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.moa.common.security.HttpSecurityCustomizer;
import com.moa.common.security.jwt.JwtAuthenticationFilter;

/**
 * 막힌 계정 확인을 JWT 필터 뒤에 둔다 (moa 018). 필터를 빈으로 두지 않는다 (서블릿 필터로 한 번 더 등록되지 않게).
 */
@Configuration
public class AccountAccessConfig {

	@Bean
	public HttpSecurityCustomizer accountAccessCustomizer(AccountAccessChecker accountAccessChecker) {
		return http -> http.addFilterAfter(new AccountAccessFilter(accountAccessChecker),
			JwtAuthenticationFilter.class);
	}
}
