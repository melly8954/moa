package com.moa.api.controller.v1.notification.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "이 브라우저의 웹 푸시 구독 (PushSubscription.toJSON())")
public record PushSubscriptionRequest(
	@Schema(description = "브라우저 푸시 서비스 주소") @NotBlank @Size(max = 1000) String endpoint,
	@Schema(description = "구독 키") @NotNull @Valid Keys keys) {

	@Schema(description = "구독 키 (base64url)")
	public record Keys(@NotBlank @Size(max = 200) String p256dh, @NotBlank @Size(max = 100) String auth) {
	}
}
