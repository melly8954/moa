package com.moa.api.controller.v1.party.response;

import java.time.OffsetDateTime;

import com.moa.api.dto.party.MessageType;
import com.moa.api.dto.party.PartyMessageDto;
import com.moa.api.dto.party.PartyMessageView;
import com.moa.api.dto.party.SystemEvent;
import com.moa.common.response.ApiTimes;

import io.swagger.v3.oas.annotations.media.Schema;

/** 채팅 메시지 (contracts/party-api.md 채팅 Message). 안내 문구는 화면이 systemEvent와 subject로 만든다 */
@Schema(description = "파티 메시지")
public record PartyMessageResponse(
	@Schema(description = "메시지 ID (커서)") Long id,
	@Schema(description = "종류") MessageType type,
	@Schema(description = "보낸 회원. SYSTEM이면 null") PartyUserResponse sender,
	@Schema(description = "내용. SYSTEM이면 null") String content,
	@Schema(description = "안내 종류. CHAT이면 null") SystemEvent systemEvent,
	@Schema(description = "안내의 대상 회원. CHAT이면 null") PartyUserResponse subject,
	@Schema(description = "보낸 시각") OffsetDateTime sentAt) {

	public static PartyMessageResponse from(PartyMessageView view) {
		PartyMessageDto message = view.message();
		return new PartyMessageResponse(message.id(), message.type(), PartyUserResponse.from(view.sender()),
			message.content(), message.systemEvent(), PartyUserResponse.from(view.subject()),
			ApiTimes.of(message.sentAt()));
	}
}
