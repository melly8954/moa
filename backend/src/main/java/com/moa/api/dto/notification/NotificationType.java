package com.moa.api.dto.notification;

/**
 * 개인 알림 종류 (specs/007-notifications research R5). 알림을 보내는 기능이 생길 때 그 기능의 스펙이 더한다
 * (예: COMMENT, INVITE, MESSAGE, SANCTION). 엔터티와 API가 함께 쓰는 값이라 dto에 둔다 (006 NoticeStatus와 같은 이유)
 */
public enum NotificationType {
	/** 운영자가 한 사용자에게 알리는 시스템 알림 */
	SYSTEM,
	/** 승인 파티에 참가 신청이 옴. 받는 사람: 방장 (specs/008-party FR-029) */
	PARTY_APPLIED,
	/** 참가 신청이 승인됨. 받는 사람: 신청자 */
	PARTY_APPROVED,
	/** 참가 신청이 거절됨. 받는 사람: 신청자 */
	PARTY_REJECTED,
	/** 방장을 이어받음. 받는 사람: 새 방장 */
	PARTY_HOST_CHANGED,
	/** 파티에서 내보내짐. 받는 사람: 내보내진 회원 */
	PARTY_KICKED,
	/** 방장이 파티를 해산함. 받는 사람: 방장 말고 멤버와 대기 신청자 */
	PARTY_DISSOLVED
}
