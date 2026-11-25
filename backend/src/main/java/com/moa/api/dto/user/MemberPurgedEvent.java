package com.moa.api.dto.user;

import java.time.LocalDateTime;

/**
 * "파기됨" (specs/018-member-withdrawal FR-017). 탈퇴했거나 관리자가 지운 회원의 개인정보를 파기했다. 개인정보를 지우는 처리는 여기에 붙인다.
 * 트랜잭션 안에서 낸다. 이 스펙은 내기만 하고 받는 쪽은 없다.
 *
 * @param userId 회원 ID
 * @param purgedAt 파기한 시각
 */
public record MemberPurgedEvent(Long userId, LocalDateTime purgedAt) {
}
