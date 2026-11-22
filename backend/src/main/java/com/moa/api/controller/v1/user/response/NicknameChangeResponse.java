package com.moa.api.controller.v1.user.response;

import java.time.OffsetDateTime;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.user.NicknameChangeDto;
import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "닉네임 바꾸기 결과")
public record NicknameChangeResponse(
	@Schema(description = "닉네임", example = "모아") String nickname,
	@Schema(description = "닉네임 태그", example = "0421") @Nullable String nicknameTag,
	@Schema(description = "다음에 닉네임을 바꿀 수 있는 시각. 지금 바꿀 수 있으면 null") @Nullable OffsetDateTime nicknameChangeableAt) {

	public static NicknameChangeResponse from(NicknameChangeDto result) {
		return new NicknameChangeResponse(result.nickname(), result.nicknameTag(),
			ApiTimes.of(result.nicknameChangeableAt()));
	}
}
