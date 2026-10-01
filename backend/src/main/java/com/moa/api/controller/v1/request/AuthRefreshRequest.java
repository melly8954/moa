package com.moa.api.controller.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 재발급·로그아웃 요청. 앱만 본문을 보낸다. 웹은 쿠키로 보내므로 본문이 없다")
public record AuthRefreshRequest(
	@Schema(description = "리프레시 토큰 (앱)") String refreshToken) {
}
