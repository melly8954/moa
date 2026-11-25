package com.moa.api.service.user;

import java.util.Optional;

import com.moa.api.entity.user.User;

/**
 * 다른 사람에게 보이는 회원 이름 (specs/018-member-withdrawal FR-008, research R8).
 * 탈퇴(유예 중)했거나 파기된 회원은 User의 기본 조회에 나오지 않는다. 조회에 없는 회원을 "탈퇴한 회원"으로 바꾸는 한 곳이다.
 */
public final class MemberDisplay {

	/** 파기한 회원 행의 이름도 이 값으로 덮는다 (docs/privacy.md "탈퇴 회원 파기") */
	public static final String WITHDRAWN_NAME = "탈퇴한 회원";

	private MemberDisplay() {
	}

	/** @param user 기본 조회로 찾은 회원. 없으면 탈퇴했거나 지워진 회원이다 */
	public static String nameOf(Optional<User> user) {
		return user.map(User::getName).orElse(WITHDRAWN_NAME);
	}
}
