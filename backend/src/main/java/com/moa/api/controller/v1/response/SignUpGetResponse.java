package com.moa.api.controller.v1.response;

import java.time.Instant;

import com.moa.api.dto.SignUpDto;
import com.moa.common.oauth.OAuthProvider;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "가입 진행 상태. 가입하는 본인에게만 준다")
public record SignUpGetResponse(
	@Schema(description = "계정 이메일. 카카오가 이메일을 주지 않아 아직 정해지지 않았으면 null") String email,
	@Schema(description = "구글·카카오 가입이면 제공자, 이메일 가입이면 null") OAuthProvider provider,
	@Schema(description = "휴대폰 인증을 마쳤는지") boolean phoneVerified,
	@Schema(description = "인증한 휴대폰 번호가 기존 계정과 겹치면 안내 정보, 아니면 null") PhoneConflict phoneConflict,
	@Schema(description = "가입 진행 만료 시각") Instant expiresAt) {

	public static SignUpGetResponse from(SignUpDto dto) {
		PhoneConflict conflict = dto.phoneConflict() == null
			? null
			: new PhoneConflict(dto.phoneConflict().accountStatus(), dto.phoneConflict().maskedEmail());
		return new SignUpGetResponse(dto.email(), dto.provider(), dto.phoneVerified(), conflict, dto.expiresAt());
	}

	@Schema(description = "휴대폰 번호 중복 안내")
	public record PhoneConflict(
		@Schema(description = "겹친 계정의 상태 (ACTIVE, SUSPENDED, WITHDRAWN, PURGED)") String accountStatus,
		@Schema(description = "가린 기존 계정 이메일. ACTIVE·SUSPENDED일 때만") String maskedEmail) {
	}
}
