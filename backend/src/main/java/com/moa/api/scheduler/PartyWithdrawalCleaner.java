package com.moa.api.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.moa.api.application.v1.party.PartyApplication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 탈퇴했는데 파티 멤버 행이나 대기 신청이 남은 회원을 매일 다시 정리한다 (specs/008-party FR-031, research R15).
 * 탈퇴 리스너(PartyWithdrawalListener)가 실패한 경우를 치운다. cleanup-zone(기본 Asia/Seoul)의 05:15에 돌아,
 * 파기(05:20) 앞이다. 회원마다 따로 처리해 한 명이 실패해도 나머지는 정리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PartyWithdrawalCleaner {

	private final PartyApplication partyApplication;

	@Scheduled(cron = "${app.party.withdrawal-cleanup-cron:0 15 5 * * *}",
		zone = "${app.withdrawal.cleanup-zone:Asia/Seoul}")
	public void cleanUp() {
		List<Long> userIds = partyApplication.findWithdrawalLeftovers();
		int cleaned = 0;
		for (Long userId : userIds) {
			try {
				partyApplication.leaveByWithdrawal(userId);
				cleaned++;
			} catch (RuntimeException e) {
				log.error("탈퇴한 회원의 파티 정리에 실패했습니다: userId={}", userId, e);
			}
		}
		log.info("탈퇴한 회원 {}명의 남은 파티 멤버·신청을 정리했습니다.", cleaned);
	}
}
