package com.moa.api.entity.party;

import com.moa.api.dto.party.MessageType;
import com.moa.api.dto.party.SystemEvent;
import com.moa.common.entity.BaseEntity;
import com.moa.common.entity.HardDelete;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 파티 메시지 (specs/008-party US9). 회원이 보낸 CHAT과 참가·나감 안내 SYSTEM이 있다.
 * 안내 문구는 저장하지 않고 화면이 system_event와 대상 회원 이름으로 만든다.
 */
@Getter
@Entity
@Table(name = "tb_party_message")
@HardDelete(reason = "지우는 기능이 없고 파티가 해산돼도 남긴다 (research.md R12)")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartyMessage extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "party_id", nullable = false)
	private Long partyId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MessageType type;

	@Column(name = "sender_user_id")
	private Long senderUserId;

	@Column(length = 500)
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(name = "system_event", length = 30)
	private SystemEvent systemEvent;

	@Column(name = "subject_user_id")
	private Long subjectUserId;

	private PartyMessage(Long partyId, MessageType type, Long senderUserId, String content, SystemEvent systemEvent,
		Long subjectUserId) {
		this.partyId = partyId;
		this.type = type;
		this.senderUserId = senderUserId;
		this.content = content;
		this.systemEvent = systemEvent;
		this.subjectUserId = subjectUserId;
	}

	public static PartyMessage chat(Long partyId, Long senderUserId, String content) {
		return new PartyMessage(partyId, MessageType.CHAT, senderUserId, content, null, null);
	}

	public static PartyMessage system(Long partyId, SystemEvent event, Long subjectUserId) {
		return new PartyMessage(partyId, MessageType.SYSTEM, null, null, event, subjectUserId);
	}
}
