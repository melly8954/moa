package com.moa.api.application.v1.user;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.moa.api.dto.user.MemberWithdrawnEvent;
import com.moa.api.realtime.RealtimeSessions;
import com.moa.api.service.notification.NotificationService;
import com.moa.api.service.user.MemberActivityPolicy;
import com.moa.api.service.user.MemberWithdrawalService;
import com.moa.api.service.user.WithdrawalProperties;
import com.moa.common.security.token.AuthTokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회원 탈퇴 흐름 (specs/018-member-withdrawal research R4·R9).
 *
 * <p>
 * 한 트랜잭션: 관리자 계정 막기(A201) → 행 잠금·닉네임 확인·탈퇴 → 리프레시 토큰 모두 무효 → 푸시 구독 모두 삭제(FR-021) → "탈퇴됨" 이벤트.
 * 커밋 뒤 이 서버의 실시간 연결을 닫는다. 다른 요청은 {@code AccountAccessFilter}가 다음 요청부터 막는다.
 * 로그에는 회원 ID만 남긴다.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberWithdrawalApplication {

	private final MemberActivityPolicy memberActivityPolicy;
	private final MemberWithdrawalService memberWithdrawalService;
	private final AuthTokenService authTokenService;
	private final NotificationService notificationService;
	private final RealtimeSessions realtimeSessions;
	private final WithdrawalProperties withdrawalProperties;
	private final ApplicationEventPublisher eventPublisher;
	private final TransactionTemplate transactionTemplate;

	/**
	 * @return 완전 삭제 예정 시각. 이미 탈퇴했으면(동시에 두 번) 처음 탈퇴의 예정 시각
	 */
	public LocalDateTime withdraw(Long userId, String nickname) {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime withdrawnAt = transactionTemplate.execute(status -> {
			memberActivityPolicy.requireMypageAllowed(userId);
			LocalDateTime at = memberWithdrawalService.withdraw(userId, nickname, now);
			// 서비스는 이미 탈퇴했으면 그때의 시각을 돌려준다. 이번 요청이 탈퇴시킨 것일 때만 정리와 이벤트를 한다
			if (at.equals(now)) {
				authTokenService.revokeAll(userId);
				notificationService.deleteSubscriptionsOf(userId);
				eventPublisher.publishEvent(new MemberWithdrawnEvent(userId, at));
			}
			return at;
		});
		realtimeSessions.closeAll(userId);
		log.info("회원 탈퇴: userId={}", userId);
		return withdrawalProperties.purgeAt(withdrawnAt);
	}
}
