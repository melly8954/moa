package com.moa.api.dto.party;

/** 보는 회원과 파티의 관계 (contracts/party-api.md PartyDetail.me) */
public enum PartyRelation {
	/** 아무 관계 없음 (로그인하지 않은 사람 포함) */
	NONE,
	/** 멤버 */
	MEMBER,
	/** 방장 */
	HOST,
	/** 대기 중인 신청이 있음 */
	PENDING,
	/** 내보내졌거나 거절됨 */
	BANNED
}
