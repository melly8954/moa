package com.moa.api.application.v1.notification;

import com.moa.api.dto.notification.NotificationDto;

/** 개인 알림을 만들었다. 커밋 뒤 실시간·웹 푸시로 보낸다 (research R4) */
public record NotificationCreatedEvent(NotificationDto notification) {
}
