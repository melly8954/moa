package com.moa.api.dto.notice;

/**
 * 공지 상태. 게시한 공지는 임시 저장으로 되돌리지 않는다.
 * 엔터티와 API가 함께 쓰는 값이라 dto에 둔다 (컨트롤러는 entity 패키지에 기대지 않는다. Role과 같은 이유)
 */
public enum NoticeStatus {
	/** 임시 저장. 공지 관리에만 보인다 */
	DRAFT,
	/** 게시. 사용자 목록·상세에 보인다 */
	PUBLISHED
}
