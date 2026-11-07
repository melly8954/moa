package com.moa.api.dto.loginhistory;

import java.time.LocalDateTime;

import com.moa.common.helper.loginhistory.LoginHistory;

public record LoginHistoryDto(Long id, Long userId, String loginId, boolean success, String failureCode, String ip,
	String deviceType, String os, String browser, LocalDateTime createdAt) {

	public static LoginHistoryDto from(LoginHistory history) {
		return new LoginHistoryDto(history.getId(), history.getUserId(), history.getLoginId(), history.isSuccess(),
			history.getFailureCode(), history.getIp(), history.getDeviceType(), history.getOs(), history.getBrowser(),
			history.getCreatedAt());
	}
}
