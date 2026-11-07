package com.moa.common.security.permission;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * 시작할 때 컨트롤러의 @RequirePermission 값이 모두 등록된 권한인지 본다. 오타나 등록 누락이면 시작이 실패한다.
 * 확인하지 않으면 그 API는 ADMIN도 부를 수 없게 된다.
 */
@Component
public class RequirePermissionCheck implements SmartInitializingSingleton {

	private final PermissionRegistry registry;
	private final RequestMappingHandlerMapping handlerMapping;

	public RequirePermissionCheck(PermissionRegistry registry,
		@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
		this.registry = registry;
		this.handlerMapping = handlerMapping;
	}

	@Override
	public void afterSingletonsInstantiated() {
		List<String> unknown = new ArrayList<>();
		handlerMapping.getHandlerMethods().values().forEach(handler -> {
			Method method = handler.getMethod();
			RequirePermission annotation = AnnotatedElementUtils.findMergedAnnotation(method, RequirePermission.class);
			if (annotation == null) {
				annotation = AnnotatedElementUtils.findMergedAnnotation(handler.getBeanType(), RequirePermission.class);
			}
			if (annotation != null && !registry.contains(annotation.value())) {
				unknown.add(annotation.value() + " (" + handler.getBeanType().getSimpleName() + "." + method.getName()
					+ ")");
			}
		});
		if (!unknown.isEmpty()) {
			throw new IllegalStateException("등록되지 않은 권한을 @RequirePermission에 썼습니다. PermissionSource 빈으로 등록하세요: "
				+ String.join(", ", unknown));
		}
	}
}
