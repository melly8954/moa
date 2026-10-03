package com.moa.support.external;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.moa.common.sms.SmsMessage;
import com.moa.common.sms.SmsSender;

/**
 * 문자 발송 포트의 테스트 대역. 보낸 문자를 모아 두고 테스트가 인증 번호를 읽는다.
 */
public class RecordingSmsSender implements SmsSender {

	private final List<SmsMessage> sent = new CopyOnWriteArrayList<>();

	@Override
	public void send(SmsMessage message) {
		sent.add(message);
	}

	public List<SmsMessage> sent() {
		return List.copyOf(sent);
	}

	public void clear() {
		sent.clear();
	}
}
