package com.moa.api.controller.v1.notification.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "안 읽은 개인 알림 수")
public record UnreadCountResponse(@Schema(description = "안 읽은 수") long count) {
}
