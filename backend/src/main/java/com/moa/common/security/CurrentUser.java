package com.moa.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * 로그인한 사용자를 컨트롤러 파라미터로 받는다. 예: {@code me(@CurrentUser AuthPrincipal user)}.
 * 접근을 막지는 않는다. 로그인 없이 부를 수 있는 API(@PermitAll)에서는 null이다.
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@AuthenticationPrincipal
public @interface CurrentUser {
	// 속성 없음. @AuthenticationPrincipal을 짧게 쓰는 이름이다
}
