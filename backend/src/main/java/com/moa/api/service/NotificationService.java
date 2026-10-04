package com.moa.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moa.api.entity.Notification;
import com.moa.api.entity.NotificationTargetType;
import com.moa.api.entity.NotificationType;
import com.moa.api.repository.writer.NotificationWriter;

import lombok.RequiredArgsConstructor;

/**
 * 알림을 만든다. 알림 사건을 일으킨 Service가 같은 트랜잭션 안에서 부른다.
 * 목록·배지·읽음 처리와 실시간 전달은 알림 목록 기능이 더한다.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

	private final NotificationWriter notificationWriter;

	/**
	 * 받는 회원의 알림 목록에 알림 하나를 더한다.
	 *
	 * @param memberId 받는 회원
	 * @param type 알림 사건
	 * @param actorId 알림을 일으킨 회원. 시스템·관리자 조치면 null
	 * @param targetType 알림을 누르면 갈 대상의 종류. 없으면 null
	 * @param targetId 대상 ID. 없으면 null
	 */
	@Transactional
	public void create(Long memberId, NotificationType type, Long actorId, NotificationTargetType targetType,
		Long targetId) {
		notificationWriter.create(new Notification(memberId, type, actorId, targetType, targetId));
	}
}
