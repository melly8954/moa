package com.moa.common.sms;

/**
 * 문자를 보내지 못했다. 어댑터가 던지고, Service가 S003으로 바꾼다.
 */
public class SmsSendException extends RuntimeException {

	public SmsSendException(String message, Throwable cause) {
		super(message, cause);
	}
}
