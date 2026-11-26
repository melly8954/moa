package com.moa.api.application.v1.user;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.moa.api.dto.user.MemberRestoredEvent;
import com.moa.api.dto.user.UserDto;
import com.moa.api.service.user.MemberWithdrawalService;
import com.moa.common.security.LoginAttemptedEvent;
import com.moa.common.security.recovery.RecoveryTicket;
import com.moa.common.security.token.AuthTokenService;
import com.moa.common.security.token.IssuedTokens;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 탈퇴한 계정 복구 (specs/018-member-withdrawal research R5). 복구 쿠키(본인 확인에 성공한 자리에서만 준다)로만 한다.
 *
 * <p>
 * 한 트랜잭션: 행 잠금·상태 확인·복구 → 리프레시 토큰 모두 무효 → "복구됨" 이벤트. 커밋 뒤 로그인과 같이 토큰을 발급하고 접속 이력에 성공으로 남긴다.
 * 탈퇴 때 지운 푸시 구독은 돌아오지 않는다 (US1-AC11). 로그에는 회원 ID만 남긴다.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountRecoveryApplication {

	private final MemberWithdrawalService memberWithdrawalService;
	private final AuthTokenService authTokenService;
	private final ApplicationEventPublisher eventPublisher;
	private final TransactionTemplate transactionTemplate;

	/** @return 완전 삭제 예정 시각 */
	public LocalDateTime getRecovery(Optional<RecoveryTicket> cookie) {
		RecoveryTicket ticket = memberWithdrawalService.requireTicket(cookie);
		return memberWithdrawalService.requireRecoverable(ticket, LocalDateTime.now());
	}

	public IssuedTokens recover(Optional<RecoveryTicket> cookie) {
		RecoveryTicket ticket = memberWithdrawalService.requireTicket(cookie);
		LocalDateTime now = LocalDateTime.now();
		UserDto user = transactionTemplate.execute(status -> {
			UserDto restored = memberWithdrawalService.restore(ticket, now);
			authTokenService.revokeAll(restored.id());
			eventPublisher.publishEvent(new MemberRestoredEvent(restored.id(), now));
			return restored;
		});
		IssuedTokens tokens = authTokenService.issue(user.toPrincipal());
		eventPublisher.publishEvent(LoginAttemptedEvent.succeeded(user.loginId(), user.id()));
		log.info("회원 복구: userId={}", user.id());
		return tokens;
	}
}
