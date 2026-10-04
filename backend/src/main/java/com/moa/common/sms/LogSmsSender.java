package com.moa.common.sms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 문자를 보내지 않고 본문(인증 번호 포함)을 로그로 남긴다. 개발·테스트용 (psw.sms.provider=log, INT-SMS-001).
 * 인증 번호 출력은 security.md가 허용한 예외다. 휴대폰 번호는 가운데 4자리를 가린다.
 * 운영 프로필에서는 SmsAdapterGuard가 시작을 막는다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "psw.sms.provider", havingValue = "log", matchIfMissing = true)
public class LogSmsSender implements SmsSender {

	private static final int HEAD = 3;
	private static final int TAIL = 4;

	@Override
	public void send(SmsMessage message) {
		log.info("[문자 log 어댑터] to={} text={}", mask(message.to()), message.text());
	}

	private static String mask(String phoneNumber) {
		if (phoneNumber.length() <= HEAD + TAIL) {
			return "***";
		}
		return phoneNumber.substring(0, HEAD) + "****" + phoneNumber.substring(phoneNumber.length() - TAIL);
	}
}
