package com.moa.common.security.token;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 만료된 지 하루 지난 리프레시 토큰을 매일 지운다. 하루를 남기는 것은 재사용 감지 기록을 잠시 보존하기 위해서다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleaner {

	private final RefreshTokenRepository refreshTokenRepository;

	@Transactional
	@Scheduled(cron = "${psw.auth.refresh-token-cleanup-cron:0 30 4 * * *}")
	public void deleteExpired() {
		int deleted = refreshTokenRepository.deleteExpiredBefore(LocalDateTime.now().minusDays(1));
		log.info("만료된 리프레시 토큰 {}건을 지웠습니다.", deleted);
	}
}
