package com.moa.common.sms;

/**
 * 문자 발송 포트. Service는 이 인터페이스만 부르고, 어댑터(solapi, log)는 psw.sms.provider 설정으로 고른다.
 * 트랜잭션 밖에서 부른다. 재시도하지 않는다 (중복 발송 방지).
 */
public interface SmsSender {

	/**
	 * @throws SmsSendException 타임아웃, 오류 응답
	 */
	void send(SmsMessage message);
}
