package com.moa.common.sms;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.moa.common.security.SecureTokens;

/**
 * 솔라피 메시지 API로 문자를 보낸다 (psw.sms.provider=solapi). 인증은 API 키·시크릿의 HMAC 서명이다.
 * 중복 발송을 막으려고 재시도하지 않는다 (docs/design/architecture.md 문자 발송).
 */
@Component
@ConditionalOnProperty(name = "psw.sms.provider", havingValue = "solapi")
public class SolapiSmsSender implements SmsSender {

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
	private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);
	private static final String HMAC = "HmacSHA256";

	private final SmsProperties.Solapi solapi;
	private final RestClient restClient;

	public SolapiSmsSender(SmsProperties properties) {
		this.solapi = properties.solapi();
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
		requestFactory.setReadTimeout(READ_TIMEOUT);
		this.restClient = RestClient.builder().baseUrl(solapi.baseUrl()).requestFactory(requestFactory).build();
	}

	@Override
	public void send(SmsMessage message) {
		Map<String, Object> body = Map.of("message",
			Map.of("to", message.to(), "from", solapi.sender(), "text", message.text()));
		try {
			restClient.post()
				.uri("/messages/v4/send")
				.contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, authorization())
				.body(body)
				.retrieve()
				.toBodilessEntity();
		} catch (RestClientException ex) {
			throw new SmsSendException("솔라피 발송 실패", ex);
		}
	}

	private String authorization() {
		String date = Instant.now().toString();
		String salt = SecureTokens.newToken();
		try {
			Mac mac = Mac.getInstance(HMAC);
			mac.init(new SecretKeySpec(solapi.apiSecret().getBytes(StandardCharsets.UTF_8), HMAC));
			String signature = HexFormat.of()
				.formatHex(mac.doFinal((date + salt).getBytes(StandardCharsets.UTF_8)));
			return "HMAC-SHA256 apiKey=" + solapi.apiKey() + ", date=" + date + ", salt=" + salt
				+ ", signature=" + signature;
		} catch (GeneralSecurityException ex) {
			throw new SmsSendException("솔라피 서명 실패", ex);
		}
	}
}
