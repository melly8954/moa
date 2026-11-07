package com.moa.common.security.permission;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * 권한 하나를 요구한다. 없으면 A005, 로그인하지 않았으면 A001.
 * 컨트롤러 클래스에 붙이면 그 컨트롤러 전체에 걸린다. 값은 PermissionSource로 등록한 키여야 한다 (아니면 시작 실패).
 * 판정은 PermissionGuard가 한다. ADMIN은 언제나 통과하고, 다른 사용자는 PermissionChecker 구현(rbac 헬퍼)이 정한다.
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@permissionGuard.has(authentication, '{value}')")
public @interface RequirePermission {

	/** 권한 키 (예: CodePermissions.MANAGE) */
	String value();
}
