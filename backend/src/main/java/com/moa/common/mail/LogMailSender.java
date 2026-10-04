package com.moa.common.mail;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 메일을 보내지 않고 받는 사람(가림)과 제목만 로그로 남긴다. 테스트·기본값용 (psw.mail.provider=log).
 * 본문에는 인증 링크 토큰이 있어 남기지 않는다. 로컬에서 본문을 보려면 smtp(Mailpit)를 쓴다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "psw.mail.provider", havingValue = "log", matchIfMissing = true)
public class LogMailSender implements MailSender {

	private static final int VISIBLE = 2;

	@Override
	public void send(MailMessage message) {
		log.info("[메일 log 어댑터] to={} subject={}", mask(message.to()), message.subject());
	}

	private static String mask(String email) {
		int at = email.indexOf('@');
		if (at < 0) {
			return "***";
		}
		return email.substring(0, Math.min(VISIBLE, at)) + "***" + email.substring(at);
	}
}
