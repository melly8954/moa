package com.moa.api.dto.party;

import java.time.LocalDateTime;

import com.moa.api.entity.party.PartyMessage;

/** 파티 메시지 한 개. 보낸 사람·대상의 이름은 Application이 붙인다 */
public record PartyMessageDto(Long id, Long partyId, MessageType type, Long senderUserId, String content,
	SystemEvent systemEvent, Long subjectUserId, LocalDateTime sentAt) {

	public static PartyMessageDto from(PartyMessage message) {
		return new PartyMessageDto(message.getId(), message.getPartyId(), message.getType(),
			message.getSenderUserId(), message.getContent(), message.getSystemEvent(), message.getSubjectUserId(),
			message.getCreatedAt());
	}
}
