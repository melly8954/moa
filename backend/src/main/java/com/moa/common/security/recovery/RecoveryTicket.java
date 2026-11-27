package com.moa.common.security.recovery;

import java.time.LocalDateTime;

/**
 * 복구 쿠키에 담긴 것 (specs/018-member-withdrawal research R5). 본인 확인에 성공한 그때의 탈퇴 시각을 담아, 그 뒤 다시 탈퇴했으면 쓸 수 없게 한다.
 *
 * @param userId 회원 ID
 * @param withdrawnAt 본인 확인 때의 탈퇴 시각
 */
public record RecoveryTicket(Long userId, LocalDateTime withdrawnAt) {
}
