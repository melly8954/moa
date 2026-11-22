package com.moa.api.dto.user;

/**
 * 디스코드 태그 공개 범위 (잠정, specs/009-mypage FR-008·FR-013, research R7). 처음 값은 PRIVATE.
 */
public enum DiscordVisibility {
	/** 아무에게도 */
	PRIVATE,
	/** 친구·팔로워만. 친구·팔로우 기능이 생기기 전에는 아무에게도 보이지 않는다 */
	FRIENDS,
	/** 로그인한 회원 모두 */
	MEMBERS
}
