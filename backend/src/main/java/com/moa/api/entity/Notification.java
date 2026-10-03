package com.moa.api.entity;

import java.time.LocalDateTime;

import com.moa.common.entity.BaseTimeEntity;
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
 * 받는 회원의 서비스 안 알림 하나. 읽으면 readAt이 찬다.
 */
@Getter
@Entity
@Table(name = "notifications")
@HardDelete(reason = "보관 기간(30일)이 지나면 지운다")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 받는 회원 */
	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 50)
	private NotificationType type;

	/** 알림을 일으킨 회원. 시스템·관리자 조치면 null */
	@Column(name = "actor_id")
	private Long actorId;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", length = 30)
	private NotificationTargetType targetType;

	@Column(name = "target_id")
	private Long targetId;

	@Column(name = "read_at")
	private LocalDateTime readAt;
}
