package com.moa.api.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.moa.api.application.v1.inquiry.InquiryApplication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 탈퇴한 날부터 보관 기간(기본 365일)이 지난 회원의 1:1 문의를 매일 지운다 (specs/017-customer-center FR-017, research R10).
 * 회원 파기(05:20) 뒤 05:30에 cleanup-zone(기본 Asia/Seoul)의 시각으로 돈다. 여러 서버에서 같이 돌아도 결과가 같다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InquiryCleaner {

	private final InquiryApplication inquiryApplication;

	@Value("${app.inquiry.withdrawn-retention-days:365}")
	private int retentionDays;

	@Scheduled(cron = "${app.inquiry.cleanup-cron:0 30 5 * * *}", zone = "${app.inquiry.cleanup-zone:Asia/Seoul}")
	public void deleteOfWithdrawnMembers() {
		log.info("탈퇴 뒤 보관 기간이 지난 1:1 문의 {}개를 지웠습니다.", inquiryApplication.deleteOfWithdrawnMembers(retentionDays));
	}
}
