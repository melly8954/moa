package com.moa.common.security;

import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 요청이 {@code @PermitAll}을 붙인 컨트롤러 메서드로 가는지 본다.
 * 인증 제외 경로를 SecurityConfig에 따로 적지 않아도 된다.
 */
@Slf4j
@RequiredArgsConstructor
public class PermitAllRequestMatcher implements RequestMatcher {

	private final RequestMappingHandlerMapping handlerMapping;

	@Override
	public boolean matches(HttpServletRequest request) {
		try {
			HandlerExecutionChain chain = handlerMapping.getHandler(request);
			return chain != null && chain.getHandler() instanceof HandlerMethod handlerMethod
				&& PermitAllSupport.isPermitAll(handlerMethod);
		} catch (Exception ex) {
			log.debug("핸들러를 찾지 못했습니다: {}", request.getRequestURI(), ex);
			return false;
		}
	}
}
