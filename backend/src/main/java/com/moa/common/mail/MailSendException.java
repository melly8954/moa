package com.moa.common.mail;

/**
 * 메일을 보내지 못했다. 어댑터가 던지고, Service가 S002로 바꾼다.
 */
public class MailSendException extends RuntimeException {

	public MailSendException(String message, Throwable cause) {
		super(message, cause);
	}
}
