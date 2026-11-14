package com.moa.api.controller.v1.notification.response;

import java.time.OffsetDateTime;

import com.moa.api.dto.notification.NotificationDto;
import com.moa.api.dto.notification.NotificationType;
import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "개인 알림")
public record NotificationResponse(
	@Schema(description = "알림 ID") Long id,
	@Schema(description = "종류") NotificationType type,
	@Schema(description = "제목") String title,
	@Schema(description = "관련 화면 경로", example = "/notices/7") String link,
	@Schema(description = "만든 시각") OffsetDateTime createdAt,
	@Schema(description = "읽었나") boolean read) {

	public static NotificationResponse from(NotificationDto dto) {
		return new NotificationResponse(dto.id(), dto.type(), dto.title(), dto.link(), ApiTimes.of(dto.createdAt()),
			dto.read());
	}
}
