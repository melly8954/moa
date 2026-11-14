package com.moa.api.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.moa.api.application.v1.notification.NotificationApplication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 보관 기간(기본 90일)이 지난 개인 알림을 매일 지운다 (specs/007-notifications FR-009, research R9).
 * 여러 서버에서 같이 돌아도 결과가 같다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleaner {

	private final NotificationApplication notificationApplication;

	@Value("${app.notification.retention-days:90}")
	private int retentionDays;

	@Scheduled(cron = "${app.notification.cleanup-cron:0 0 5 * * *}")
	public void deleteExpired() {
		log.info("보관 기간이 지난 알림 {}개를 지웠습니다.", notificationApplication.deleteExpired(retentionDays));
	}
}
