package com.moa.api.dto.party;

/**
 * 파티 채팅 안내 종류 (specs/008-party FR-020). 문구는 화면이 만든다. 엔터티와 API가 함께 쓰는 값이라 dto에 둔다
 */
public enum SystemEvent {
	/** 들어옴 */
	JOINED,
	/** 나감 */
	LEFT,
	/** 내보내짐 */
	KICKED,
	/** 방장이 바뀜 */
	HOST_CHANGED
}
