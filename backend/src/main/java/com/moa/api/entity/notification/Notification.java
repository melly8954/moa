package com.moa.api.entity.notification;

import java.time.LocalDateTime;

import com.moa.api.dto.notification.NotificationType;
import com.moa.common.entity.HardDelete;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 개인 알림 (specs/007-notifications). 받는 사람만 읽음을 바꾼다. 보내는 쪽이 시스템일 수 있어 감사 필드를 두지 않는다.
 */
@Getter
@Entity
@Table(name = "notifications")
@HardDelete(reason = "보관 기간(90일)이 지난 알림은 정리기가 지운다. 사용자가 지우는 기능은 없다")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private NotificationType type;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, length = 500)
	private String link;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "read_at")
	private LocalDateTime readAt;

	public Notification(Long userId, NotificationType type, String title, String link, LocalDateTime createdAt) {
		this.userId = userId;
		this.type = type;
		this.title = title;
		this.link = link;
		this.createdAt = createdAt;
	}

	public boolean isRead() {
		return readAt != null;
	}

	/** 읽음으로 바꾼다. 이미 읽었으면 그대로 둔다 */
	public void read(LocalDateTime now) {
		if (readAt == null) {
			readAt = now;
		}
	}
}
