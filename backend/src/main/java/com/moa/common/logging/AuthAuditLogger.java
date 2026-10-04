package com.moa.common.logging;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import lombok.extern.slf4j.Slf4j;

/**
 * 로그인 감사 로그 (docs/design/security.md 감사 로그). 항목: 회원 ID 또는 입력 이메일, 수단, IP, 결과.
 * traceId는 로그 패턴이 함께 남긴다. 비밀번호·토큰·인증 번호·휴대폰 번호는 남기지 않는다.
 */
@Slf4j
@Component
public class AuthAuditLogger {

	private static final String UNKNOWN_IP = "-";

	/**
	 * @param method 로그인 수단 (PASSWORD, GOOGLE, KAKAO)
	 * @param via 로그인한 경로 (가입 완료, 이메일 인증 연결, 계정 합치기, 연결된 계정)
	 */
	public void signInSucceeded(Long memberId, String method, String via) {
		log.info("[감사] 로그인 성공 memberId={} method={} via={} ip={} result=SUCCESS", memberId, method, via,
			clientIp());
	}

	/**
	 * @param identifier 회원 ID, 입력 이메일, 또는 알 수 없으면 -
	 * @param method 로그인 수단 (PASSWORD, GOOGLE, KAKAO)
	 * @param errorCode 실패 사유 오류 코드
	 */
	public void signInFailed(String identifier, String method, String errorCode) {
		log.info("[감사] 로그인 실패 identifier={} method={} ip={} result=FAILURE code={}", identifier, method,
			clientIp(), errorCode);
	}

	private static String clientIp() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
			return attributes.getRequest().getRemoteAddr();
		}
		return UNKNOWN_IP;
	}
}
