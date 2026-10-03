package com.moa.support.external;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 외부 연동(메일, 문자, OAuth) 포트를 테스트 대역으로 바꾼다 (conventions.md 7절 외부 연동).
 * 설정으로 고른 어댑터가 있어도 @Primary로 이 대역이 쓰인다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ExternalTestDoubleConfig {

	@Bean
	@Primary
	public RecordingMailSender recordingMailSender() {
		return new RecordingMailSender();
	}

	@Bean
	@Primary
	public RecordingSmsSender recordingSmsSender() {
		return new RecordingSmsSender();
	}

	@Bean
	@Primary
	public StubOAuthClient stubOAuthClient() {
		return new StubOAuthClient();
	}
}
