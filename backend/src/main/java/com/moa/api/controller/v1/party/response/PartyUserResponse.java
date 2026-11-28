package com.moa.api.controller.v1.party.response;

import com.moa.api.dto.party.MemberRef;

import io.swagger.v3.oas.annotations.media.Schema;

/** 응답의 회원. 로그인 아이디는 싣지 않는다 (specs/008-party FR-027) */
@Schema(description = "파티 회원")
public record PartyUserResponse(
	@Schema(description = "회원 ID") Long userId,
	@Schema(description = "표시 이름 (닉네임). 탈퇴한 회원은 \"탈퇴한 회원\"") String displayName) {

	public static PartyUserResponse from(MemberRef ref) {
		return ref == null ? null : new PartyUserResponse(ref.userId(), ref.displayName());
	}
}
