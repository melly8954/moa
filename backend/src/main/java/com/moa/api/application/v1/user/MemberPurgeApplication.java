package com.moa.api.application.v1.user;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.moa.api.dto.user.MemberPurgedEvent;
import com.moa.api.service.notice.NoticeService;
import com.moa.api.service.notification.NotificationService;
import com.moa.api.service.user.MemberPurgeService;
import com.moa.common.purgelog.PersonalDataPurgedEvent;
import com.moa.common.purgelog.PurgeMethod;
import com.moa.common.purgelog.PurgeTarget;
import com.moa.common.security.token.RefreshTokenEraser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 유예가 끝난 회원 파기 (specs/018-member-withdrawal research R6·R9).
 *
 * <p>
 * 회원마다 트랜잭션 하나: 행 잠금·다시 확인 → 회원 도메인 행 삭제·회원 행 덮기 → 알림·푸시 구독 → 공지 확인 → 리프레시 토큰 →
 * 파기 이력(PersonalDataPurgedEvent, 동기라 저장이 실패하면 그 회원의 파기가 되돌아간다) → "파기됨" 이벤트.
 * 한 회원이 실패하면 회원 ID와 예외 클래스 이름만 로그에 남기고 다음 회원으로 간다. 다음 날 다시 찾는다.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberPurgeApplication {

	private final MemberPurgeService memberPurgeService;
	private final NotificationService notificationService;
	private final NoticeService noticeService;
	private final RefreshTokenEraser refreshTokenEraser;
	private final ApplicationEventPublisher eventPublisher;
	private final TransactionTemplate transactionTemplate;

	/** @return 파기한 회원 수 */
	public int purgeDue() {
		LocalDateTime now = LocalDateTime.now();
		int purged = 0;
		for (Long userId : memberPurgeService.findDueIds(now)) {
			try {
				if (Boolean.TRUE.equals(transactionTemplate.execute(status -> purge(userId, now)))) {
					purged++;
				}
			} catch (RuntimeException ex) {
				log.warn("회원 파기 실패: userId={}, error={}", userId, ex.getClass().getSimpleName());
			}
		}
		return purged;
	}

	/** 유예가 끝난 뒤 며칠이 지나도 남은 회원 수 (정리기가 ERROR로 알린다) */
	public int countOverdue() {
		return memberPurgeService.countOverdue(LocalDateTime.now());
	}

	private boolean purge(Long userId, LocalDateTime now) {
		if (!memberPurgeService.eraseIfDue(userId, now)) {
			return false;
		}
		notificationService.deleteAllOf(userId);
		noticeService.deleteReadOf(userId);
		refreshTokenEraser.deleteAllOf(userId);
		eventPublisher.publishEvent(
			new PersonalDataPurgedEvent(PurgeTarget.MEMBER, PurgeMethod.COLUMN_ERASE, 1, null, userId));
		eventPublisher.publishEvent(new MemberPurgedEvent(userId, now));
		return true;
	}
}
