package com.moa.common.sms;

/**
 * 보낼 문자 한 통.
 *
 * @param to 받는 휴대폰 번호(숫자만, 예: 01012345678)
 * @param text 본문
 */
public record SmsMessage(String to, String text) {
}
