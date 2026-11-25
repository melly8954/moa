package com.moa.api.dto.user;

import java.time.LocalDateTime;

/**
 * "탈퇴됨" (specs/018-member-withdrawal FR-017). 회원이 스스로 탈퇴했다. 30일 안에 되돌릴 수 있으므로 되돌릴 수 있는 처리만 붙인다.
 * 트랜잭션 안에서 낸다. 이 스펙은 내기만 하고 받는 쪽은 없다.
 *
 * @param userId 회원 ID
 * @param withdrawnAt 탈퇴한 시각
 */
public record MemberWithdrawnEvent(Long userId, LocalDateTime withdrawnAt) {
}
