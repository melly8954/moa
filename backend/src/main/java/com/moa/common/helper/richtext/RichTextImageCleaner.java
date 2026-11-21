package com.moa.common.helper.richtext;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.moa.common.purgelog.PersonalDataPurgedEvent;
import com.moa.common.purgelog.PurgeTarget;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 글에 쓰이지 않은(임시) 본문 이미지를 매일 지운다 (선택 헬퍼 rich-text). 여러 서버에서 같이 돌아도 결과가 같다.
 * scheduler 헬퍼가 있으면 @SchedulerLock을 더해 한 서버에서만 돌게 할 수 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RichTextImageCleaner {

	private final RichTextImageService richTextImageService;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	@Scheduled(cron = "${app.rich-text.cleanup-cron:0 50 4 * * *}")
	public void deleteExpired() {
		int deleted = richTextImageService.deleteExpiredTemps();
		log.info("쓰이지 않은 본문 이미지 {}개를 지웠습니다.", deleted);
		// moa: 파기 기록 (specs/013-purge-log). 키트로 옮길 것. 파일을 먼저 지우는 순서는 그대로다 (FR-004)
		eventPublisher.publishEvent(PersonalDataPurgedEvent.byScheduler(PurgeTarget.RICH_TEXT_IMAGE, deleted));
	}
}
