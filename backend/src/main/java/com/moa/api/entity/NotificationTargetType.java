package com.moa.api.entity;

/**
 * 알림이 가리키는 대상의 종류. 대상 ID(targetId)는 이 종류의 테이블 ID다. 다른 대상은 해당 기능에서 더한다.
 */
public enum NotificationTargetType {
	/** 연결된 소셜 계정 (member_social_accounts) */
	MEMBER_SOCIAL_ACCOUNT
}
