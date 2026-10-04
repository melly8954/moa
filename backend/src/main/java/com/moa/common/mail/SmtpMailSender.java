package com.moa.common.mail;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * SMTP로 메일을 보낸다 (psw.mail.provider=smtp). 접속·타임아웃은 spring.mail.*에서 정한다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "psw.mail.provider", havingValue = "smtp")
public class SmtpMailSender implements MailSender {

	private final JavaMailSender javaMailSender;
	private final MailProperties mailProperties;

	@Override
	public void send(MailMessage message) {
		SimpleMailMessage mail = new SimpleMailMessage();
		mail.setFrom(mailProperties.from());
		mail.setTo(message.to());
		mail.setSubject(message.subject());
		mail.setText(message.body());
		try {
			javaMailSender.send(mail);
		} catch (MailException ex) {
			throw new MailSendException("SMTP 발송 실패", ex);
		}
	}
}
