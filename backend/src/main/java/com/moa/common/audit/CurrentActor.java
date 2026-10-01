package com.moa.common.audit;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import com.moa.common.security.AuthPrincipal;
import com.moa.common.security.PermitAllSupport;

import lombok.RequiredArgsConstructor;

/**
 * 감사 행위자를 정한다. 행위자는 비지 않는다 (harness-psw 4.7).
 *
 * <ol>
 *   <li>로그인 사용자가 있으면 그 사용자 ID</li>
 *   <li>SystemActor 안이거나 @PermitAll API 요청이면 시스템 계정 ID (psw.audit.system-actor-id)</li>
 *   <li>둘 다 아니면 없음. BaseEntity가 저장을 막아 인증 누락 버그를 조용히 넘기지 않는다</li>
 * </ol>
 */
@Component("auditorAware")
@RequiredArgsConstructor
public class CurrentActor implements AuditorAware<Long> {

	public static final String MISSING_ACTOR = "감사 행위자를 정할 수 없습니다. 로그인 없이 저장하는 작업은 "
		+ "SystemActor로 감싸거나 API에 @PermitAll을 붙입니다.";

	private final AuditProperties auditProperties;

	/**
	 * @throws IllegalStateException 행위자를 정할 수 없을 때
	 */
	public Long id() {
		return find().orElseThrow(() -> new IllegalStateException(MISSING_ACTOR));
	}

	public Optional<Long> find() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof AuthPrincipal principal) {
			return Optional.of(principal.userId());
		}
		if (SystemActor.isActive() || isPermitAllRequest()) {
			return Optional.of(auditProperties.systemActorId());
		}
		return Optional.empty();
	}

	/**
	 * 행위자 필드가 없는 엔터티(BaseTimeEntity)에도 불리므로 여기서는 예외를 던지지 않는다.
	 * 행위자가 비었는지는 BaseEntity가 저장 직전에 검사한다.
	 */
	@Override
	public Optional<Long> getCurrentAuditor() {
		return find();
	}

	private boolean isPermitAllRequest() {
		if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
			return false;
		}
		Object handler = attributes.getRequest().getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
		return handler instanceof HandlerMethod handlerMethod && PermitAllSupport.isPermitAll(handlerMethod);
	}
}
