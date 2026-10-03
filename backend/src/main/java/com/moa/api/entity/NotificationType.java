package com.moa.api.entity;

/**
 * 알림 사건. 사건 목록의 정본은 docs/req/functional/notification/_policy.md 알림 사건이다.
 * 이름은 {@code <대상>_<사건>}으로 짓는다 (예: FOLLOW_REQUESTED, PARTY_INVITED). 다른 사건은 해당 기능에서 더한다.
 */
public enum NotificationType {
	/** 구글·카카오 계정이 회원 계정에 연결됐다 (계정 합치기, 이메일 인증 연결로 카카오 연결 포함) */
	LOGIN_METHOD_LINKED,
	/** 비밀번호가 회원 계정에 추가됐다 (이메일 인증 연결로 비밀번호 추가 포함) */
	LOGIN_METHOD_ADDED
}
