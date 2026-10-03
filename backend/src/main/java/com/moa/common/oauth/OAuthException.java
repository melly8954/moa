package com.moa.common.oauth;

/**
 * 제공자 인증에 실패했다. 어댑터가 던지고, Service가 A007로 바꾼다.
 */
public class OAuthException extends RuntimeException {

	public OAuthException(String message, Throwable cause) {
		super(message, cause);
	}
}
