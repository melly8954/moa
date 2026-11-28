package com.moa.api.application.v1.party;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.moa.api.dto.user.MemberWithdrawnEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회원이 탈퇴하면 속한 파티에서 나가고 대기 신청을 취소한다 (specs/008-party FR-031, research R15).
 * 탈퇴가 커밋된 뒤 새 트랜잭션(PartyApplication.leaveByWithdrawal)에서 돈다. 실패해도 탈퇴는 그대로이고,
 * 남은 멤버 행·신청은 PartyWithdrawalCleaner가 매일 다시 정리한다 (018 FR-017).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PartyWithdrawalListener {

	private final PartyApplication partyApplication;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onWithdrawn(MemberWithdrawnEvent event) {
		try {
			partyApplication.leaveByWithdrawal(event.userId());
		} catch (RuntimeException e) {
			log.error("탈퇴한 회원의 파티 정리에 실패했습니다. 정리기가 다시 합니다: userId={}", event.userId(), e);
		}
	}
}
