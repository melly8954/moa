package com.moa.api.dto.party;

/**
 * 응답에 싣는 회원. 로그인 아이디는 싣지 않는다 (specs/008-party FR-027)
 *
 * @param displayName 닉네임. 탈퇴한 회원은 "탈퇴한 회원"
 */
public record MemberRef(Long userId, String displayName) {
}
