package com.moa.common.purgelog;

/**
 * 파기 대상 종류 (specs/013-purge-log FR-002). 새 정리를 붙이는 스펙이 값을 더한다.
 */
public enum PurgeTarget {
	/** 접속 이력 */
	LOGIN_HISTORY,
	/** 알림 */
	NOTIFICATION,
	/** 리프레시 토큰 */
	REFRESH_TOKEN,
	/** 글에 쓰이지 않은 본문 이미지 */
	RICH_TEXT_IMAGE
}
