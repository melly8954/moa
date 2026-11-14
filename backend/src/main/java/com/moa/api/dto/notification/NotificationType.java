package com.moa.api.dto.notification;

/**
 * 개인 알림 종류 (specs/007-notifications research R5). 알림을 보내는 기능이 생길 때 그 기능의 스펙이 더한다
 * (예: COMMENT, INVITE, MESSAGE, SANCTION). 엔터티와 API가 함께 쓰는 값이라 dto에 둔다 (006 NoticeStatus와 같은 이유)
 */
public enum NotificationType {
	/** 운영자가 한 사용자에게 알리는 시스템 알림 */
	SYSTEM
}
