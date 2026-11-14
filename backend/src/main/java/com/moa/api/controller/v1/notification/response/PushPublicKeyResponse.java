package com.moa.api.controller.v1.notification.response;

import org.jspecify.annotations.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "브라우저 알림(웹 푸시) VAPID 공개 키")
public record PushPublicKeyResponse(
	@Schema(description = "base64url 공개 키. 서버에서 웹 푸시를 끄면 없다") @Nullable String publicKey) {
}
