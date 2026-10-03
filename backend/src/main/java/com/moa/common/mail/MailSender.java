package com.moa.common.mail;

/**
 * 메일 발송 포트. Service는 이 인터페이스만 부르고, 어댑터(smtp, ses, log)는 psw.mail.provider 설정으로 고른다.
 * 트랜잭션 밖에서 부른다.
 */
public interface MailSender {

	/**
	 * @throws MailSendException 연결 실패, 타임아웃, 서버 거부
	 */
	void send(MailMessage message);
}
