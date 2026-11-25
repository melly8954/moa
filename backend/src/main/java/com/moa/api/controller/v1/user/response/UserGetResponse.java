package com.moa.api.controller.v1.user.response;

import com.moa.api.dto.user.MeDto;
import com.moa.api.dto.user.UserDto;
import com.moa.common.security.Role;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자")
public record UserGetResponse(
	@Schema(description = "사용자 ID", example = "3") Long id,
	@Schema(description = "로그인 아이디", example = "user") String loginId,
	@Schema(description = "이름", example = "사용자") String name,
	@Schema(description = "역할", example = "USER") Role role,
	@Schema(description = "임시 비밀번호를 받아 바꿔야 한다. true면 화면이 비밀번호 변경으로 보낸다") boolean passwordChangeRequired,
	@Schema(
		description = "moa: 비밀번호가 있는지. 없으면(소셜 회원) 비밀번호 변경을 숨긴다 (010 US2-AC3)") boolean hasPassword,
	@Schema(description = "moa: 계정 바 아바타 키. 프로필이 없거나 목록에서 빠진 키면 default (009 FR-014)",
		example = "default") String avatar,
	@Schema(
		description = "moa: 마이페이지를 쓸 수 있는지. 관리자 계정(ADMIN)이면 거짓 (009 FR-016)") boolean mypageAllowed) {

	public static UserGetResponse from(MeDto me) {
		UserDto user = me.user();
		return new UserGetResponse(user.id(), user.loginId(), user.name(), user.role(), user.passwordChangeRequired(),
			user.hasPassword(), me.avatar(), me.mypageAllowed());
	}
}
