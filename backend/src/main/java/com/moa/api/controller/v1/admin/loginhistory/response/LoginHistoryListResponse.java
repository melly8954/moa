package com.moa.api.controller.v1.admin.loginhistory.response;

import java.time.OffsetDateTime;

import org.jspecify.annotations.Nullable;

import com.moa.api.dto.loginhistory.LoginHistoryDto;
import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "접속 이력")
public record LoginHistoryListResponse(
	@Schema(description = "이력 ID") Long id,
	@Schema(description = "사용자 ID. 실패하면 없다") @Nullable Long userId,
	@Schema(description = "입력한 아이디") String loginId,
	@Schema(description = "성공 여부") boolean success,
	@Schema(description = "실패 코드", example = "A006") @Nullable String failureCode,
	@Schema(description = "접속 IP") @Nullable String ip,
	@Schema(description = "기기 종류", example = "PC") String deviceType,
	@Schema(description = "OS", example = "Windows") String os,
	@Schema(description = "브라우저", example = "Chrome") String browser,
	@Schema(description = "시각 (시간대 포함)") OffsetDateTime createdAt) {

	public static LoginHistoryListResponse from(LoginHistoryDto dto) {
		return new LoginHistoryListResponse(dto.id(), dto.userId(), dto.loginId(), dto.success(), dto.failureCode(),
			dto.ip(), dto.deviceType(), dto.os(), dto.browser(), ApiTimes.of(dto.createdAt()));
	}
}
