package com.moa.common.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * 보안 필터 체인에 설정을 더한다 (예: OAuth 로그인). 빈으로 등록하면 SecurityConfig가 골격 설정 뒤에 적용한다.
 * 골격의 기본 규칙(인증 필수, JSON 오류 응답)은 바꾸지 않는다.
 */
@FunctionalInterface
public interface HttpSecurityCustomizer {

	void customize(HttpSecurity http) throws Exception;
}
