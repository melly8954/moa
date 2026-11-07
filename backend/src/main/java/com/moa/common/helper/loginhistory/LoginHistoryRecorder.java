package com.moa.common.helper.loginhistory;

import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.moa.common.security.LoginAttemptedEvent;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 로그인 시도(LoginAttemptedEvent)를 접속 이력으로 남긴다. 저장에 실패해도 로그인은 막지 않는다.
 * IP는 request.getRemoteAddr()다. 프록시 뒤라면 server.forward-headers-strategy=native로 실제 IP를 받는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginHistoryRecorder {

	private final LoginHistoryRepository loginHistoryRepository;

	@EventListener
	public void record(LoginAttemptedEvent event) {
		HttpServletRequest request = currentRequest();
		String ip = request == null ? null : request.getRemoteAddr();
		String userAgent = request == null ? null : request.getHeader(HttpHeaders.USER_AGENT);
		try {
			loginHistoryRepository.save(new LoginHistory(event.userId(), event.loginId(), event.success(),
				event.failureCode(), ip, userAgent));
		} catch (RuntimeException ex) {
			log.error("접속 이력을 남기지 못했습니다. loginId={}", event.loginId(), ex);
		}
	}

	private HttpServletRequest currentRequest() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
			return attributes.getRequest();
		}
		return null;
	}
}
