package com.moa.common.mail;

/**
 * 보낼 메일 한 통.
 *
 * @param to 받는 주소
 * @param subject 제목
 * @param body 본문(텍스트)
 */
public record MailMessage(String to, String subject, String body) {
}
