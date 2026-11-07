package com.moa.common.helper.loginhistory;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 보관 기간이 지난 접속 이력을 매일 지운다. 여러 서버에서 같이 돌아도 결과가 같다.
 * scheduler 헬퍼가 있으면 @SchedulerLock을 더해 한 서버에서만 돌게 할 수 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginHistoryCleaner {

	private final LoginHistoryRepository loginHistoryRepository;

	@Value("${app.login-history.retention-days:365}")
	private int retentionDays;

	@Transactional
	@Scheduled(cron = "${app.login-history.cleanup-cron:0 40 4 * * *}")
	public void deleteExpired() {
		int deleted = loginHistoryRepository.deleteCreatedBefore(LocalDateTime.now().minusDays(retentionDays));
		log.info("보관 기간({}일)이 지난 접속 이력 {}건을 지웠습니다.", retentionDays, deleted);
	}
}
