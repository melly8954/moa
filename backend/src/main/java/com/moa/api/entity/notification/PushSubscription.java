package com.moa.api.entity.notification;

import java.time.LocalDateTime;

import com.moa.common.entity.HardDelete;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 브라우저 알림(웹 푸시) 구독 (specs/007-notifications research R8). 한 사용자가 브라우저마다 하나를 가진다.
 */
@Getter
@Entity
@Table(name = "push_subscriptions")
@HardDelete(reason = "브라우저 알림을 끄거나 로그아웃하거나 푸시 서버가 만료를 알리면 지운다. 이력이 아니다")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushSubscription {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false, length = 1000)
	private String endpoint;

	@Column(nullable = false, length = 200)
	private String p256dh;

	@Column(nullable = false, length = 100)
	private String auth;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	public PushSubscription(Long userId, String endpoint, String p256dh, String auth, LocalDateTime createdAt) {
		this.userId = userId;
		this.endpoint = endpoint;
		this.p256dh = p256dh;
		this.auth = auth;
		this.createdAt = createdAt;
	}

	/** 같은 브라우저(endpoint)를 다른 사용자가 켜면 그 사용자 것으로 바꾼다 (공유 PC) */
	public void rebind(Long userId, String p256dh, String auth) {
		this.userId = userId;
		this.p256dh = p256dh;
		this.auth = auth;
	}
}
