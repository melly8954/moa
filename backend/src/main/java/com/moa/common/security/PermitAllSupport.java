package com.moa.common.security;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;

import jakarta.annotation.security.PermitAll;

/**
 * 로그인 없이 부를 수 있는 API는 컨트롤러 메서드나 클래스에 {@code @PermitAll}을 붙인다.
 * 보안 설정(인증 제외)과 감사(행위자를 시스템 계정으로)가 같은 표시를 읽는다.
 */
public final class PermitAllSupport {

	private PermitAllSupport() {
	}

	public static boolean isPermitAll(HandlerMethod handlerMethod) {
		return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), PermitAll.class)
			|| AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), PermitAll.class);
	}
}
