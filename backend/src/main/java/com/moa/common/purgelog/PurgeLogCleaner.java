package com.moa.common.purgelog;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 보관 기간(기본 1년)이 지난 파기 기록을 매일 지운다 (specs/013-purge-log FR-006). 여러 서버에서 같이 돌아도 결과가 같다.
 * 파기 기록에는 개인정보가 없으므로 이 정리는 파기 기록을 남기지 않는다(이벤트를 내지 않는다, docs/privacy.md "파기 기록").
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PurgeLogCleaner {

	private final PurgeLogRepository purgeLogRepository;

	@Value("${app.purge-log.retention-days:365}")
	private int retentionDays;

	@Transactional
	@Scheduled(cron = "${app.purge-log.cleanup-cron:0 10 5 * * *}")
	public void deleteExpired() {
		int deleted = purgeLogRepository.deleteCreatedBefore(LocalDateTime.now().minusDays(retentionDays));
		log.info("보관 기간({}일)이 지난 파기 기록 {}건을 지웠습니다.", retentionDays, deleted);
	}
}
