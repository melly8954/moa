package com.moa.api.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.moa.api.application.v1.user.MemberPurgeApplication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 유예가 끝난 회원을 매일 파기한다 (specs/018-member-withdrawal FR-013·FR-019, research R6).
 * 서버 시간대와 상관없이 cleanup-zone(기본 Asia/Seoul)의 05:20에 돈다. 다른 정리(04:30~05:10) 뒤다.
 * 여러 서버에서 같이 돌아도 회원 행 잠금 뒤 다시 확인하므로 회원마다 한 번만 파기된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberPurgeCleaner {

	private final MemberPurgeApplication memberPurgeApplication;

	@Scheduled(cron = "${app.withdrawal.cleanup-cron:0 20 5 * * *}",
		zone = "${app.withdrawal.cleanup-zone:Asia/Seoul}")
	public void purgeDue() {
		log.info("유예가 끝난 회원 {}명을 파기했습니다.", memberPurgeApplication.purgeDue());
		int overdue = memberPurgeApplication.countOverdue();
		if (overdue > 0) {
			log.error("유예가 끝난 뒤 며칠이 지나도 파기되지 않은 회원이 {}명 있습니다.", overdue);
		}
	}
}
