package com.moa.common.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 메일 발송 설정. SMTP 접속 정보는 spring.mail.*(MAIL_HOST 등)에 둔다.
 *
 * @param provider 어댑터: smtp 또는 log (MAIL_PROVIDER)
 * @param from 보내는 주소 (MAIL_FROM)
 */
@Validated
@ConfigurationProperties("psw.mail")
public record MailProperties(@NotBlank @Pattern(regexp = "smtp|log") String provider, @NotBlank String from) {
}
