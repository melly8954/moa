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
	RICH_TEXT_IMAGE,
	/** 회원 (탈퇴·관리자 삭제, specs/018-member-withdrawal) */
	MEMBER,
	/** 탈퇴한 날부터 1년이 지난 회원의 1:1 문의 (specs/017-customer-center FR-017) */
	INQUIRY
}
