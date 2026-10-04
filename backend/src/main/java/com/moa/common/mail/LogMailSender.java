package com.moa.common.mail;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 메일을 보내지 않고 로그로 남긴다. 테스트·로컬용 (psw.mail.provider=log).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "psw.mail.provider", havingValue = "log", matchIfMissing = true)
public class LogMailSender implements MailSender {

	@Override
	public void send(MailMessage message) {
		log.info("[메일 log 어댑터] to={} subject={}\n{}", message.to(), message.subject(), message.body());
	}
}
