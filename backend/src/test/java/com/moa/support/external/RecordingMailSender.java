package com.moa.support.external;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.moa.common.mail.MailMessage;
import com.moa.common.mail.MailSender;

/**
 * 메일 발송 포트의 테스트 대역. 보낸 메일을 모아 두고 테스트가 내용을 확인한다.
 */
public class RecordingMailSender implements MailSender {

	private final List<MailMessage> sent = new CopyOnWriteArrayList<>();

	@Override
	public void send(MailMessage message) {
		sent.add(message);
	}

	public List<MailMessage> sent() {
		return List.copyOf(sent);
	}

	public void clear() {
		sent.clear();
	}
}
