package com.moa.api.controller.v1.party.request;

import java.util.List;

import com.moa.api.dto.party.PartyForm;

/** 만들기·고치기 요청을 PartyForm으로 바꾼다 */
final class PartyRequests {

	private PartyRequests() {
	}

	static PartyForm form(String gameCode, String modeCode, String tierMinCode, String tierMaxCode, String title,
		String description, Integer capacity, List<PartyPositionRequest> positions, String hostPositionCode) {
		return new PartyForm(gameCode, modeCode, tierMinCode, tierMaxCode, title, description, capacity,
			positions == null ? List.of() : positions.stream().map(PartyPositionRequest::toSlot).toList(),
			hostPositionCode);
	}
}
