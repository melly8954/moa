package com.moa.common.sms;

import java.util.Arrays;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * 운영(prod) 프로필에서 문자 log 어댑터를 켜면 시작을 막는다 (docs/design/security.md 금지·제약).
 */
@Component
@RequiredArgsConstructor
public class SmsAdapterGuard {

	private static final String PROD = "prod";

	private final Environment environment;
	private final SmsProperties smsProperties;

	@PostConstruct
	void check() {
		boolean prod = Arrays.asList(environment.getActiveProfiles()).contains(PROD);
		if (prod && "log".equals(smsProperties.provider())) {
			throw new IllegalStateException("운영 프로필에서는 문자 log 어댑터를 쓸 수 없습니다. SMS_PROVIDER=solapi로 둡니다.");
		}
	}
}
