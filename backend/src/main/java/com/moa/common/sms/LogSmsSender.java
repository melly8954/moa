package com.moa.common.sms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 문자를 보내지 않고 본문(인증 번호 포함)을 로그로 남긴다. 개발·테스트용 (psw.sms.provider=log, INT-SMS-001).
 * 운영 프로필에서는 SmsAdapterGuard가 시작을 막는다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "psw.sms.provider", havingValue = "log", matchIfMissing = true)
public class LogSmsSender implements SmsSender {

	@Override
	public void send(SmsMessage message) {
		log.info("[문자 log 어댑터] to={} text={}", message.to(), message.text());
	}
}
