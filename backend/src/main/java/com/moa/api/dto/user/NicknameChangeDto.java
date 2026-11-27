package com.moa.api.dto.user;

import java.time.LocalDateTime;

/**
 * 닉네임 바꾸기 결과 (specs/009-mypage contracts "닉네임 바꾸기").
 *
 * @param nicknameTag 프로필 행이 없는 회원이 지금 이름 그대로 보냈으면 null
 * @param nicknameChangeableAt 다음에 바꿀 수 있는 시각. 지금 바꿀 수 있으면 null
 */
public record NicknameChangeDto(String nickname, String nicknameTag, LocalDateTime nicknameChangeableAt) {
}
