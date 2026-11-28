package com.moa.api.dto.party;

/**
 * 메시지 한 개. 보낸 사람과 대상의 표시 이름을 붙였다 (contracts/party-api.md 채팅 Message)
 *
 * @param sender CHAT이면 보낸 회원. SYSTEM이면 null
 * @param subject SYSTEM이면 대상 회원. CHAT이면 null
 */
public record PartyMessageView(PartyMessageDto message, MemberRef sender, MemberRef subject) {
}
