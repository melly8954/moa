package com.moa.common.helper.webpush;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 브라우저 알림(웹 푸시) 설정 (moa 007, 키트 헬퍼로 옮길 것). 공개 키·개인 키는 base64url로 인코딩한 VAPID 키다.
 * 둘 중 하나라도 비면 웹 푸시를 끈다. 키는 사람이 만들어 환경 변수로만 준다.
 *
 * @param publicKey VAPID 공개 키 (브라우저 구독 때 applicationServerKey로 쓴다)
 * @param privateKey VAPID 개인 키
 * @param subject 푸시 서버가 연락할 운영자 주소 (mailto:...)
 */
@ConfigurationProperties("app.web-push")
public record WebPushProperties(String publicKey, String privateKey, String subject) {

	public boolean enabled() {
		return publicKey != null && !publicKey.isBlank() && privateKey != null && !privateKey.isBlank();
	}
}
