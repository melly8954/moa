package com.moa.api.dto.party;

/**
 * 파티 메시지 종류. 엔터티와 API가 함께 쓰는 값이라 dto에 둔다
 */
public enum MessageType {
	/** 회원이 보낸 메시지 */
	CHAT,
	/** 참가·나감 등 안내 */
	SYSTEM
}
