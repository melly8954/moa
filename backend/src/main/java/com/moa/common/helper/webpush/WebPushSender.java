package com.moa.common.helper.webpush;

import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.jwt.JwtFactory;

/**
 * 브라우저 알림(웹 푸시)을 보낸다 (moa 007, 키트 헬퍼로 옮길 것). 라이브러리 dev.blanke.webpush가 페이로드 암호화
 * (aes128gcm)를, jjwt가 VAPID 서명(ES256)을 한다 (specs/007-notifications research R6).
 */
@Slf4j
@Component
public class WebPushSender {

	/** BouncyCastle 키는 알고리즘 이름이 ECDH라 jjwt가 아는 JDK EC 키로 바꿔 서명한다 */
	private static final JwtFactory JJWT = (header, claims, key) -> Jwts.builder()
		.header().add("typ", header.get("typ")).and()
		.claims(claims)
		.signWith(asJdkEcKey(key), Jwts.SIG.ES256)
		.compact();

	private final WebPushProperties properties;
	private final PushService pushService;

	public WebPushSender(WebPushProperties properties) throws GeneralSecurityException {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
		this.properties = properties;
		this.pushService = properties.enabled()
			? PushService.builder()
				.withVapidPublicKey(properties.publicKey())
				.withVapidPrivateKey(properties.privateKey())
				.withVapidSubject(properties.subject())
				.withJwtFactory(JJWT)
				.build()
			: null;
	}

	public boolean enabled() {
		return pushService != null;
	}

	/** VAPID 공개 키 (브라우저 구독용). 꺼져 있으면 null */
	public String publicKey() {
		return enabled() ? properties.publicKey() : null;
	}

	/**
	 * 구독 하나에 페이로드를 보낸다. 실패해도 던지지 않고 결과로 알린다.
	 *
	 * @param endpoint 브라우저 푸시 서비스 주소
	 * @param p256dh 브라우저 공개 키 (base64url)
	 * @param auth 인증 비밀 (base64url)
	 * @param payload 서비스 워커가 받을 JSON
	 */
	public WebPushResult send(String endpoint, String p256dh, String auth, String payload) {
		if (!enabled()) {
			return WebPushResult.DISABLED;
		}
		try {
			Notification notification = Notification.builder()
				.endpoint(endpoint)
				.userPublicKey(p256dh)
				.userAuth(auth)
				.payload(payload)
				.build();
			HttpResponse<Void> response = pushService.send(notification, Encoding.AES_128_GCM,
				HttpResponse.BodyHandlers.discarding());
			int status = response.statusCode();
			if (status == 404 || status == 410) {
				return WebPushResult.EXPIRED;
			}
			if (status >= 200 && status < 300) {
				return WebPushResult.SENT;
			}
			log.warn("웹 푸시를 보내지 못했습니다: 상태 {}", status);
			return WebPushResult.FAILED;
		} catch (Exception ex) {
			log.warn("웹 푸시를 보내지 못했습니다", ex);
			return WebPushResult.FAILED;
		}
	}

	private static PrivateKey asJdkEcKey(ECPrivateKey key) {
		try {
			return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(key.getEncoded()));
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("VAPID 개인 키를 읽지 못했습니다", ex);
		}
	}
}
