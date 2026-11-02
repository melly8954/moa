package com.moa.api.entity;

/**
 * 회원 계정 상태.
 */
public enum MemberStatus {
	/** 정상 */
	ACTIVE,
	/** 관리자 제재로 정지. 로그인할 수 없다 */
	SUSPENDED,
	/** 탈퇴 후 유예 중. 로그인할 수 없다 */
	WITHDRAWN,
	/** 개인정보를 파기했다 */
	PURGED
}
