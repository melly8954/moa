package com.moa.api.controller.v1.party.response;

import java.util.List;

import com.moa.api.dto.party.PartyMessagePageView;

import io.swagger.v3.oas.annotations.media.Schema;

/** 메시지 한 묶음 (research.md R5). 쪽 번호가 아니라 id 커서다 */
@Schema(description = "메시지 묶음")
public record PartyMessagePageResponse(
	@Schema(description = "메시지. 오래된 것부터") List<PartyMessageResponse> items,
	@Schema(description = "이 방향으로 더 있는지") boolean hasMore) {

	public static PartyMessagePageResponse from(PartyMessagePageView view) {
		return new PartyMessagePageResponse(view.items().stream().map(PartyMessageResponse::from).toList(),
			view.hasMore());
	}
}
