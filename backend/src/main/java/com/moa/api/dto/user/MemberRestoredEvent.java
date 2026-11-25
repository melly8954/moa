package com.moa.api.dto.user;

import java.time.LocalDateTime;

/**
 * "복구됨" (specs/018-member-withdrawal FR-017). 탈퇴한 회원이 유예 기간 안에 복구했다.
 * 트랜잭션 안에서 낸다. 이 스펙은 내기만 하고 받는 쪽은 없다.
 *
 * @param userId 회원 ID
 * @param restoredAt 복구한 시각
 */
public record MemberRestoredEvent(Long userId, LocalDateTime restoredAt) {
}
