package com.moa.api.dto.user;

import java.time.LocalDateTime;

/**
 * "탈퇴됨" (specs/018-member-withdrawal FR-017). 회원이 스스로 탈퇴했다. 30일 안에 복구할 수 있으므로 되돌릴 수 있는 처리만 붙인다
 * (숨기기 등). 개인정보를 지우는 처리는 "파기됨"(MemberPurgedEvent)에 붙인다.
 * 트랜잭션 안에서 낸다. 이 스펙은 내기만 하고 받는 쪽은 없다.
 *
 * <p>
 * 붙이는 방법: {@code @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)}로 받는다. 저장이 커밋된 뒤에만 돌고,
 * 처리가 실패해도 탈퇴·복구·파기는 되돌아가지 않는다(US4-AC4). 실패 기록과 다시 시도는 붙이는 기능이 맡는다.
 * 이벤트에는 회원 내부 ID와 시각만 있다. 필요한 값은 받는 기능이 자기 데이터에서 찾는다.
 * </p>
 *
 * @param userId 회원 ID
 * @param withdrawnAt 탈퇴한 시각
 */
public record MemberWithdrawnEvent(Long userId, LocalDateTime withdrawnAt) {
}
